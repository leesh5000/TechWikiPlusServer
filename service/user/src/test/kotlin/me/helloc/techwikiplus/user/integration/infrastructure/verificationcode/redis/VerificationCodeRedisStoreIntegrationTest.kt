package me.helloc.techwikiplus.user.integration.infrastructure.verificationcode.redis

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.exception.CustomException.AuthenticationException.ExpiredEmailVerification
import me.helloc.techwikiplus.user.infrastructure.config.IntegrationTestSupport
import me.helloc.techwikiplus.user.infrastructure.config.TestContainerConfig
import me.helloc.techwikiplus.user.infrastructure.verificationcode.redis.VerificationCodeRedisStore
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration

class VerificationCodeRedisStoreIntegrationTest : IntegrationTestSupport() {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            TestContainerConfig.properties(registry)
        }
    }

    @Autowired
    private lateinit var verificationCodeStore: VerificationCodeRedisStore

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 Redis 데이터 초기화
        redisTemplate.connectionFactory?.connection?.flushAll()
    }

    @Nested
    @DisplayName("인증 코드 저장")
    inner class StoreVerificationCodeTest {
        @Test
        fun `인증 코드를 저장하고 TTL을 설정한다`() {
            // given
            val email = "test@example.com"
            val code = VerificationCode.generate()
            val ttl = Duration.ofMinutes(5)

            // when
            verificationCodeStore.storeWithExpiry(email, code, ttl)

            // then
            val key = "user:verification:email:$email"
            val storedValue = redisTemplate.opsForValue().get(key)
            val remainingTtl = redisTemplate.getExpire(key)

            assertThat(storedValue).isEqualTo(code.value)
            assertThat(remainingTtl).isNotNull
            assertThat(remainingTtl).isNotEqualTo(-1L) // -1은 TTL이 설정되지 않았음을 의미
            assertThat(remainingTtl).isNotEqualTo(-2L) // -2는 키가 존재하지 않음을 의미
        }

        @Test
        fun `같은 이메일로 새로운 코드를 저장하면 기존 코드를 덮어쓴다`() {
            // given
            val email = "update@example.com"
            val firstCode = VerificationCode("FIRST1")
            val secondCode = VerificationCode("SECOND")
            val ttl = Duration.ofMinutes(5)

            // when
            verificationCodeStore.storeWithExpiry(email, firstCode, ttl)
            verificationCodeStore.storeWithExpiry(email, secondCode, ttl)

            // then
            val retrievedCode = verificationCodeStore.retrieveOrThrows(email)
            assertThat(retrievedCode.value).isEqualTo("SECOND")
        }

        @Test
        fun `서로 다른 이메일의 코드는 독립적으로 저장된다`() {
            // given
            val email1 = "user1@example.com"
            val email2 = "user2@example.com"
            val code1 = VerificationCode("CODE11")
            val code2 = VerificationCode("CODE22")
            val ttl = Duration.ofMinutes(5)

            // when
            verificationCodeStore.storeWithExpiry(email1, code1, ttl)
            verificationCodeStore.storeWithExpiry(email2, code2, ttl)

            // then
            val retrieved1 = verificationCodeStore.retrieveOrThrows(email1)
            val retrieved2 = verificationCodeStore.retrieveOrThrows(email2)

            assertThat(retrieved1.value).isEqualTo("CODE11")
            assertThat(retrieved2.value).isEqualTo("CODE22")
        }

        @Test
        fun `짧은 TTL을 설정할 수 있다`() {
            // given
            val email = "short-ttl@example.com"
            val code = VerificationCode.generate()
            val ttl = Duration.ofSeconds(2) // CI 환경에서의 타이밍 문제를 고려하여 2초로 증가

            // when
            verificationCodeStore.storeWithExpiry(email, code, ttl)

            // then - 즉시 조회하면 성공
            val retrievedCode = verificationCodeStore.retrieveOrThrows(email)
            assertThat(retrievedCode.value).isEqualTo(code.value)

            // 3초 후 조회하면 만료되어 실패
            Thread.sleep(3000)
            assertThatThrownBy { verificationCodeStore.retrieveOrThrows(email) }
                .isInstanceOf(ExpiredEmailVerification::class.java)
                .extracting("email")
                .isEqualTo(email)
        }
    }

    @Nested
    @DisplayName("인증 코드 조회")
    inner class RetrieveVerificationCodeTest {
        @Test
        fun `저장된 인증 코드를 조회한다`() {
            // given
            val email = "retrieve@example.com"
            val code = VerificationCode("ABCDEF")
            val ttl = Duration.ofMinutes(10)

            verificationCodeStore.storeWithExpiry(email, code, ttl)

            // when
            val retrievedCode = verificationCodeStore.retrieveOrThrows(email)

            // then
            assertThat(retrievedCode.value).isEqualTo("ABCDEF")
        }

        @Test
        fun `존재하지 않는 이메일로 조회하면 ExpiredEmailVerification 예외가 발생한다`() {
            // given
            val email = "notfound@example.com"

            // when & then
            assertThatThrownBy { verificationCodeStore.retrieveOrThrows(email) }
                .isInstanceOf(ExpiredEmailVerification::class.java)
                .extracting("email")
                .isEqualTo(email)
        }

        @Test
        fun `만료된 코드를 조회하면 ExpiredEmailVerification 예외가 발생한다`() {
            // given
            val email = "expired@example.com"
            val code = VerificationCode.generate()
            val ttl = Duration.ofMillis(500) // 0.5초

            verificationCodeStore.storeWithExpiry(email, code, ttl)

            // when
            Thread.sleep(1000) // 1초 대기

            // then
            assertThatThrownBy { verificationCodeStore.retrieveOrThrows(email) }
                .isInstanceOf(ExpiredEmailVerification::class.java)
                .extracting("email")
                .isEqualTo(email)
        }

        @Test
        fun `조회 후에도 TTL이 유지된다`() {
            // given
            val email = "ttl-check@example.com"
            val code = VerificationCode.generate()
            val ttl = Duration.ofMinutes(5)

            verificationCodeStore.storeWithExpiry(email, code, ttl)

            // when - 첫 번째 조회
            verificationCodeStore.retrieveOrThrows(email)

            // then - Redis에서 TTL 확인
            val key = "user:verification:email:$email"
            val remainingTtl = redisTemplate.getExpire(key)

            assertThat(remainingTtl).isNotNull
            assertThat(remainingTtl).isNotEqualTo(-1L)
            assertThat(remainingTtl).isNotEqualTo(-2L)

            // 두 번째 조회도 성공해야 함
            val secondRetrieve = verificationCodeStore.retrieveOrThrows(email)
            assertThat(secondRetrieve.value).isEqualTo(code.value)
        }
    }

    @Nested
    @DisplayName("특수한 이메일 형식 처리")
    inner class SpecialEmailFormatTest {
        @Test
        fun `특수문자가 포함된 이메일도 처리한다`() {
            // given
            val specialEmails =
                listOf(
                    "user+tag@example.com",
                    "user.name@example.com",
                    "user_name@example.com",
                    "user-name@example.com",
                )

            // when & then
            specialEmails.forEach { email ->
                val code = VerificationCode.generate()
                val ttl = Duration.ofMinutes(5)

                verificationCodeStore.storeWithExpiry(email, code, ttl)
                val retrieved = verificationCodeStore.retrieveOrThrows(email)

                assertThat(retrieved.value).isEqualTo(code.value)
            }
        }

        @Test
        fun `대소문자가 다른 이메일은 서로 다른 키로 저장된다`() {
            // given
            val lowerEmail = "test@example.com"
            val upperEmail = "TEST@EXAMPLE.COM"
            val mixedEmail = "Test@Example.Com"

            val code1 = VerificationCode("LOWER1")
            val code2 = VerificationCode("UPPER2")
            val code3 = VerificationCode("MIXED3")
            val ttl = Duration.ofMinutes(5)

            // when
            verificationCodeStore.storeWithExpiry(lowerEmail, code1, ttl)
            verificationCodeStore.storeWithExpiry(upperEmail, code2, ttl)
            verificationCodeStore.storeWithExpiry(mixedEmail, code3, ttl)

            // then
            assertThat(verificationCodeStore.retrieveOrThrows(lowerEmail).value).isEqualTo("LOWER1")
            assertThat(verificationCodeStore.retrieveOrThrows(upperEmail).value).isEqualTo("UPPER2")
            assertThat(verificationCodeStore.retrieveOrThrows(mixedEmail).value).isEqualTo("MIXED3")
        }
    }

    @Nested
    @DisplayName("동시성 처리")
    inner class ConcurrencyTest {
        @Test
        fun `동시에 여러 코드를 저장해도 정상 동작한다`() {
            // given
            val emailCount = 100
            val ttl = Duration.ofMinutes(5)
            val codes = mutableMapOf<String, String>()

            // when - 100개의 이메일에 대해 동시에 코드 저장
            val threads =
                (1..emailCount).map { i ->
                    val email = "concurrent$i@example.com"
                    val code = VerificationCode.generate()
                    codes[email] = code.value

                    Thread {
                        verificationCodeStore.storeWithExpiry(email, code, ttl)
                    }
                }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            // then - 모든 코드가 올바르게 저장되었는지 확인
            codes.forEach { (email, expectedCode) ->
                val retrievedCode = verificationCodeStore.retrieveOrThrows(email)
                assertThat(retrievedCode.value).isEqualTo(expectedCode)
            }
        }
    }

    @Nested
    @DisplayName("Redis 키 형식")
    inner class RedisKeyFormatTest {
        @Test
        fun `올바른 키 형식으로 저장된다`() {
            // given
            val email = "keyformat@example.com"
            val code = VerificationCode.generate()
            val ttl = Duration.ofMinutes(5)

            // when
            verificationCodeStore.storeWithExpiry(email, code, ttl)

            // then - 예상되는 키 형식으로 직접 조회
            val expectedKey = "user:verification:email:$email"
            val storedValue = redisTemplate.opsForValue().get(expectedKey)
            assertThat(storedValue).isEqualTo(code.value)

            // 다른 형식의 키는 존재하지 않아야 함
            val wrongKey = "verification:$email"
            val wrongValue = redisTemplate.opsForValue().get(wrongKey)
            assertThat(wrongValue).isNull()
        }
    }
}
