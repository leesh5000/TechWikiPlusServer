package me.helloc.techwikiplus.user.integration.infrastructure.verificationcode.redis

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.exception.CustomException.AuthenticationException.ExpiredEmailVerification
import me.helloc.techwikiplus.user.infrastructure.config.TestContainerConfig
import me.helloc.techwikiplus.user.infrastructure.verificationcode.redis.VerificationCodeRedisStore
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestContainerConfig::class)
class VerificationCodeRedisStoreIntegrationTest(
    private val verificationCodeStore: VerificationCodeRedisStore,
    private val redisTemplate: StringRedisTemplate,
) : FunSpec({

        extensions(SpringExtension)

        beforeEach {
            // 각 테스트 전에 Redis 데이터 초기화
            redisTemplate.connectionFactory?.connection?.flushAll()
        }

        context("인증 코드 저장") {
            test("인증 코드를 저장하고 TTL을 설정한다") {
                val email = "test@example.com"
                val code = VerificationCode.generate()
                val ttl = Duration.ofMinutes(5)

                verificationCodeStore.storeWithExpiry(email, code, ttl)

                // Redis에서 직접 확인
                val key = "user:verification:email:$email"
                val storedValue = redisTemplate.opsForValue().get(key)
                val remainingTtl = redisTemplate.getExpire(key)

                storedValue shouldBe code.value
                remainingTtl shouldNotBe null
                remainingTtl!! shouldNotBe -1L // -1은 TTL이 설정되지 않았음을 의미
                remainingTtl shouldNotBe -2L // -2는 키가 존재하지 않음을 의미
            }

            test("같은 이메일로 새로운 코드를 저장하면 기존 코드를 덮어쓴다") {
                val email = "update@example.com"
                val firstCode = VerificationCode("FIRST1")
                val secondCode = VerificationCode("SECOND")
                val ttl = Duration.ofMinutes(5)

                verificationCodeStore.storeWithExpiry(email, firstCode, ttl)
                verificationCodeStore.storeWithExpiry(email, secondCode, ttl)

                val retrievedCode = verificationCodeStore.retrieveOrThrows(email)

                retrievedCode.value shouldBe "SECOND"
            }

            test("서로 다른 이메일의 코드는 독립적으로 저장된다") {
                val email1 = "user1@example.com"
                val email2 = "user2@example.com"
                val code1 = VerificationCode("CODE11")
                val code2 = VerificationCode("CODE22")
                val ttl = Duration.ofMinutes(5)

                verificationCodeStore.storeWithExpiry(email1, code1, ttl)
                verificationCodeStore.storeWithExpiry(email2, code2, ttl)

                val retrieved1 = verificationCodeStore.retrieveOrThrows(email1)
                val retrieved2 = verificationCodeStore.retrieveOrThrows(email2)

                retrieved1.value shouldBe "CODE11"
                retrieved2.value shouldBe "CODE22"
            }

            test("짧은 TTL을 설정할 수 있다") {
                val email = "short-ttl@example.com"
                val code = VerificationCode.generate()
                val ttl = Duration.ofSeconds(1)

                verificationCodeStore.storeWithExpiry(email, code, ttl)

                // 즉시 조회하면 성공
                val retrievedCode = verificationCodeStore.retrieveOrThrows(email)
                retrievedCode.value shouldBe code.value

                // 2초 후 조회하면 만료되어 실패
                Thread.sleep(2000)
                shouldThrow<ExpiredEmailVerification> {
                    verificationCodeStore.retrieveOrThrows(email)
                }.email shouldBe email
            }
        }

        context("인증 코드 조회") {
            test("저장된 인증 코드를 조회한다") {
                val email = "retrieve@example.com"
                val code = VerificationCode("ABCDEF")
                val ttl = Duration.ofMinutes(10)

                verificationCodeStore.storeWithExpiry(email, code, ttl)

                val retrievedCode = verificationCodeStore.retrieveOrThrows(email)

                retrievedCode.value shouldBe "ABCDEF"
            }

            test("존재하지 않는 이메일로 조회하면 ExpiredEmailVerification 예외가 발생한다") {
                val email = "notfound@example.com"

                val exception =
                    shouldThrow<ExpiredEmailVerification> {
                        verificationCodeStore.retrieveOrThrows(email)
                    }

                exception.email shouldBe email
            }

            test("만료된 코드를 조회하면 ExpiredEmailVerification 예외가 발생한다") {
                val email = "expired@example.com"
                val code = VerificationCode.generate()
                val ttl = Duration.ofMillis(100) // 0.1초

                verificationCodeStore.storeWithExpiry(email, code, ttl)

                Thread.sleep(200) // 0.2초 대기

                val exception =
                    shouldThrow<ExpiredEmailVerification> {
                        verificationCodeStore.retrieveOrThrows(email)
                    }

                exception.email shouldBe email
            }

            test("조회 후에도 TTL이 유지된다") {
                val email = "ttl-check@example.com"
                val code = VerificationCode.generate()
                val ttl = Duration.ofMinutes(5)

                verificationCodeStore.storeWithExpiry(email, code, ttl)

                // 첫 번째 조회
                verificationCodeStore.retrieveOrThrows(email)

                // Redis에서 TTL 확인
                val key = "user:verification:email:$email"
                val remainingTtl = redisTemplate.getExpire(key)

                remainingTtl shouldNotBe null
                remainingTtl!! shouldNotBe -1L
                remainingTtl shouldNotBe -2L

                // 두 번째 조회도 성공해야 함
                val secondRetrieve = verificationCodeStore.retrieveOrThrows(email)
                secondRetrieve.value shouldBe code.value
            }
        }

        context("특수한 이메일 형식 처리") {
            test("특수문자가 포함된 이메일도 처리한다") {
                val specialEmails =
                    listOf(
                        "user+tag@example.com",
                        "user.name@example.com",
                        "user_name@example.com",
                        "user-name@example.com",
                    )

                specialEmails.forEach { email ->
                    val code = VerificationCode.generate()
                    val ttl = Duration.ofMinutes(5)

                    verificationCodeStore.storeWithExpiry(email, code, ttl)
                    val retrieved = verificationCodeStore.retrieveOrThrows(email)

                    retrieved.value shouldBe code.value
                }
            }

            test("대소문자가 다른 이메일은 서로 다른 키로 저장된다") {
                val lowerEmail = "test@example.com"
                val upperEmail = "TEST@EXAMPLE.COM"
                val mixedEmail = "Test@Example.Com"

                val code1 = VerificationCode("LOWER1")
                val code2 = VerificationCode("UPPER2")
                val code3 = VerificationCode("MIXED3")
                val ttl = Duration.ofMinutes(5)

                verificationCodeStore.storeWithExpiry(lowerEmail, code1, ttl)
                verificationCodeStore.storeWithExpiry(upperEmail, code2, ttl)
                verificationCodeStore.storeWithExpiry(mixedEmail, code3, ttl)

                verificationCodeStore.retrieveOrThrows(lowerEmail).value shouldBe "LOWER1"
                verificationCodeStore.retrieveOrThrows(upperEmail).value shouldBe "UPPER2"
                verificationCodeStore.retrieveOrThrows(mixedEmail).value shouldBe "MIXED3"
            }
        }

        context("동시성 처리") {
            test("동시에 여러 코드를 저장해도 정상 동작한다") {
                val emailCount = 100
                val ttl = Duration.ofMinutes(5)
                val codes = mutableMapOf<String, String>()

                // 100개의 이메일에 대해 동시에 코드 저장
                (1..emailCount).map { i ->
                    val email = "concurrent$i@example.com"
                    val code = VerificationCode.generate()
                    codes[email] = code.value

                    Thread {
                        verificationCodeStore.storeWithExpiry(email, code, ttl)
                    }
                }.forEach { it.start() }

                Thread.sleep(500) // 모든 스레드가 완료될 때까지 대기

                // 모든 코드가 올바르게 저장되었는지 확인
                codes.forEach { (email, expectedCode) ->
                    val retrievedCode = verificationCodeStore.retrieveOrThrows(email)
                    retrievedCode.value shouldBe expectedCode
                }
            }
        }

        context("Redis 키 형식") {
            test("올바른 키 형식으로 저장된다") {
                val email = "keyformat@example.com"
                val code = VerificationCode.generate()
                val ttl = Duration.ofMinutes(5)

                verificationCodeStore.storeWithExpiry(email, code, ttl)

                // 예상되는 키 형식으로 직접 조회
                val expectedKey = "user:verification:email:$email"
                val storedValue = redisTemplate.opsForValue().get(expectedKey)

                storedValue shouldBe code.value

                // 다른 형식의 키는 존재하지 않아야 함
                val wrongKey = "verification:$email"
                val wrongValue = redisTemplate.opsForValue().get(wrongKey)
                wrongValue shouldBe null
            }
        }
    })
