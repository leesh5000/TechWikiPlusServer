package me.helloc.techwikiplus.service.user.application.facade

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import me.helloc.techwikiplus.service.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.service.user.domain.service.UserRegister
import me.helloc.techwikiplus.service.user.infrastructure.FakeCacheStore
import me.helloc.techwikiplus.service.user.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.service.user.infrastructure.FakeMailSender
import me.helloc.techwikiplus.service.user.infrastructure.FakePasswordEncryptor
import me.helloc.techwikiplus.service.user.infrastructure.FakeUserIdGenerator
import me.helloc.techwikiplus.service.user.infrastructure.FakeUserRepository
import me.helloc.techwikiplus.service.user.infrastructure.NoOpLockManager
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UserSignUpFacadeTest : FunSpec({

    lateinit var userSignUpFacade: UserSignUpFacade
    lateinit var userRegister: UserRegister
    lateinit var emailVerifyService: EmailVerifyService

    lateinit var fakeUserRepository: FakeUserRepository
    lateinit var fakeClockHolder: FakeClockHolder
    lateinit var fakeIdGenerator: FakeUserIdGenerator
    lateinit var fakePasswordEncryptor: FakePasswordEncryptor
    lateinit var fakeMailSender: FakeMailSender
    lateinit var fakeCacheStore: FakeCacheStore
    lateinit var fakeLockManager: NoOpLockManager

    beforeEach {
        fakeUserRepository = FakeUserRepository()
        fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
        fakeIdGenerator = FakeUserIdGenerator()
        fakePasswordEncryptor = FakePasswordEncryptor()
        fakeMailSender = FakeMailSender()
        fakeCacheStore = FakeCacheStore()
        fakeLockManager = NoOpLockManager()

        userRegister =
            UserRegister(
                clockHolder = fakeClockHolder,
                userIdGenerator = fakeIdGenerator,
                repository = fakeUserRepository,
                passwordEncryptor = fakePasswordEncryptor,
                lockManager = fakeLockManager,
            )

        emailVerifyService =
            EmailVerifyService(
                mailSender = fakeMailSender,
                cacheStore = fakeCacheStore,
            )

        userSignUpFacade =
            UserSignUpFacade(
                userRegister = userRegister,
                emailVerifyService = emailVerifyService,
            )
    }

    test("유효한 회원가입 정보로 성공적으로 회원가입이 완료되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.handle(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        val savedUsers = fakeUserRepository.getAll()
        savedUsers.size shouldBe 1

        val savedUser = savedUsers[0]
        savedUser.email shouldBe email
        savedUser.nickname shouldBe nickname
        savedUser.status shouldBe UserStatus.PENDING
        savedUser.role shouldBe UserRole.USER
        savedUser.id.value shouldBe 1000000L

        val expectedEncodedPassword = fakePasswordEncryptor.encode(password)
        savedUser.encodedPassword shouldBe expectedEncodedPassword
    }

    test("회원가입 시 이메일 인증 코드가 발송되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.handle(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        fakeMailSender.getSentMailCount() shouldBe 1
        fakeMailSender.wasSentTo(email) shouldBe true

        val sentMail = fakeMailSender.getLastSentMail()
        sentMail shouldNotBe null
        sentMail!!.to shouldBe email
        sentMail.content.subject shouldBe "TechWiki+ 회원가입 인증 코드"
        sentMail.content.body shouldNotBe null
    }

    test("회원가입 시 인증 코드가 캐시에 저장되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.handle(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        val cacheKey = "registration_code:${email.value}"
        fakeCacheStore.contains(cacheKey) shouldBe true

        val storedCode = fakeCacheStore.get(cacheKey)
        storedCode shouldNotBe null
        storedCode!!.length shouldBe 6
        storedCode.all { it.isDigit() } shouldBe true
    }

    test("회원가입 후 사용자 상태가 PENDING으로 설정되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.handle(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        val savedUser = fakeUserRepository.findBy(email)
        savedUser shouldNotBe null
        savedUser!!.status shouldBe UserStatus.PENDING
        savedUser.isPending() shouldBe true
    }

    test("비밀번호와 비밀번호 확인이 일치하지 않으면 예외가 발생해야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("DifferentPassword123!")

        // when & then
        val exception =
            shouldThrow<UserDomainException> {
                userSignUpFacade.handle(
                    email = email,
                    nickname = nickname,
                    password = password,
                    confirmPassword = confirmPassword,
                )
            }

        exception.userErrorCode shouldBe UserErrorCode.PASSWORD_MISMATCH
        fakeUserRepository.getAll().size shouldBe 0
        fakeMailSender.getSentMailCount() shouldBe 0
    }

    test("이미 존재하는 이메일로 회원가입 시도 시 예외가 발생해야 한다") {
        // given
        val existingEmail = Email("existing@example.com")
        val existingUser =
            User.create(
                id = UserId(2000001L),
                email = existingEmail,
                nickname = Nickname("existinguser"),
                encodedPassword = EncodedPassword("encoded_password"),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
            )
        fakeUserRepository.save(existingUser)

        val nickname = Nickname("newuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when & then
        val exception =
            shouldThrow<UserDomainException> {
                userSignUpFacade.handle(
                    email = existingEmail,
                    nickname = nickname,
                    password = password,
                    confirmPassword = confirmPassword,
                )
            }

        exception.userErrorCode shouldBe UserErrorCode.DUPLICATE_EMAIL
        exception.params shouldBe arrayOf(existingEmail.value)
        fakeUserRepository.getAll().size shouldBe 1
        fakeMailSender.getSentMailCount() shouldBe 0
    }

    test("이미 존재하는 닉네임으로 회원가입 시도 시 예외가 발생해야 한다") {
        // given
        val existingNickname = Nickname("existinguser")
        val existingUser =
            User.create(
                id = UserId(2000001L),
                email = Email("existing@example.com"),
                nickname = existingNickname,
                encodedPassword = EncodedPassword("encoded_password"),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
            )
        fakeUserRepository.save(existingUser)

        val email = Email("new@example.com")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when & then
        val exception =
            shouldThrow<UserDomainException> {
                userSignUpFacade.handle(
                    email = email,
                    nickname = existingNickname,
                    password = password,
                    confirmPassword = confirmPassword,
                )
            }

        exception.userErrorCode shouldBe UserErrorCode.DUPLICATE_NICKNAME
        exception.params shouldBe arrayOf(existingNickname.value)
        fakeUserRepository.getAll().size shouldBe 1
        fakeMailSender.getSentMailCount() shouldBe 0
    }

    test("회원가입 프로세스가 올바른 순서로 실행되어야 한다") {
        // given
        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        var userRegisterCalled = false
        var emailVerifyCalled = false
        var callOrder = mutableListOf<String>()

        // 각 단계에서 호출 순서를 기록하기 위한 검증
        fakeUserRepository =
            object : FakeUserRepository() {
                override fun save(user: User): User {
                    if (!userRegisterCalled && user.status == UserStatus.PENDING) {
                        userRegisterCalled = true
                        callOrder.add("userRegister")
                    }
                    return super.save(user)
                }
            }

        fakeMailSender =
            object : FakeMailSender() {
                override fun send(
                    to: Email,
                    content: me.helloc.techwikiplus.service.user.domain.model.MailContent,
                ) {
                    emailVerifyCalled = true
                    callOrder.add("emailVerify")
                    super.send(to, content)
                }
            }

        userRegister =
            UserRegister(
                clockHolder = fakeClockHolder,
                userIdGenerator = fakeIdGenerator,
                repository = fakeUserRepository,
                passwordEncryptor = fakePasswordEncryptor,
                lockManager = fakeLockManager,
            )

        emailVerifyService =
            EmailVerifyService(
                mailSender = fakeMailSender,
                cacheStore = fakeCacheStore,
            )

        userSignUpFacade =
            UserSignUpFacade(
                userRegister = userRegister,
                emailVerifyService = emailVerifyService,
            )

        // when
        userSignUpFacade.handle(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        callOrder shouldBe listOf("userRegister", "emailVerify")
        userRegisterCalled shouldBe true
        emailVerifyCalled shouldBe true
    }

    test("회원가입 시 생성 시간과 수정 시간이 올바르게 설정되어야 한다") {
        // given
        val fixedTime = Instant.parse("2025-01-15T10:30:00Z")
        fakeClockHolder.setFixedTime(fixedTime)

        val email = Email("test@example.com")
        val nickname = Nickname("testuser")
        val password = RawPassword("Password123!")
        val confirmPassword = RawPassword("Password123!")

        // when
        userSignUpFacade.handle(
            email = email,
            nickname = nickname,
            password = password,
            confirmPassword = confirmPassword,
        )

        // then
        val savedUser = fakeUserRepository.findBy(email)
        savedUser shouldNotBe null
        savedUser!!.createdAt shouldBe fixedTime
        savedUser.modifiedAt shouldBe fixedTime
    }

    test("동시에 여러 회원가입이 발생해도 각각 고유한 ID를 가져야 한다") {
        // given
        val users =
            listOf(
                Triple(Email("user1@example.com"), Nickname("user1"), RawPassword("Password1!")),
                Triple(Email("user2@example.com"), Nickname("user2"), RawPassword("Password2!")),
                Triple(Email("user3@example.com"), Nickname("user3"), RawPassword("Password3!")),
                Triple(Email("user4@example.com"), Nickname("user4"), RawPassword("Password4!")),
                Triple(Email("user5@example.com"), Nickname("user5"), RawPassword("Password5!")),
            )

        val latch = CountDownLatch(users.size)
        val exceptions = mutableListOf<Exception>()
        val failureDetails = mutableListOf<String>()

        // when: 실제 동시 실행
        val futures =
            users.mapIndexed { index, (email, nickname, password) ->
                CompletableFuture.supplyAsync {
                    try {
                        println("[Thread-$index] Starting signup for ${email.value}")
                        userSignUpFacade.handle(
                            email = email,
                            nickname = nickname,
                            password = password,
                            confirmPassword = password,
                        )
                        // 회원가입 성공 후 저장된 사용자 조회
                        val user = fakeUserRepository.findBy(email)
                        println("[Thread-$index] Completed signup for ${email.value} with ID: ${user?.id?.value}")
                        user
                    } catch (e: Exception) {
                        synchronized(exceptions) {
                            exceptions.add(e)
                            failureDetails.add("[Thread-$index] Failed for ${email.value}: ${e.message}")
                            println("[Thread-$index] Exception for ${email.value}: ${e.message}")
                            e.printStackTrace()
                        }
                        null
                    } finally {
                        latch.countDown()
                    }
                }
            }

        // 모든 스레드가 완료될 때까지 대기 (최대 15초)
        val completed = latch.await(15, TimeUnit.SECONDS)

        // 타임아웃 체크
        if (!completed) {
            println("WARNING: Timeout occurred, some threads may not have completed")
        }

        // 모든 결과 수집
        val results =
            futures.mapNotNull {
                try {
                    it.get()
                } catch (e: Exception) {
                    println("Error getting future result: ${e.message}")
                    null
                }
            }

        // 디버깅 정보 출력
        println("=== Test Results Debug Info ===")
        println("Expected users: ${users.size}")
        println("Successful results: ${results.size}")
        println("Exceptions count: ${exceptions.size}")
        println("Failure details: $failureDetails")
        println("Mail sent count: ${fakeMailSender.getSentMailCount()}")
        println("Repository size: ${fakeUserRepository.getAll().size}")

        // then - 더 관대한 검증
        if (exceptions.isNotEmpty()) {
            println("Exceptions occurred during test:")
            exceptions.forEach { e ->
                println("- ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        // 핵심 검증: ID 고유성 (예외가 있어도 성공한 것들은 고유해야 함)
        val userIds = results.map { it.id.value }
        userIds.distinct().size shouldBe userIds.size // 모든 ID가 고유해야 함

        // 모든 ID가 예상 범위 내에 있는지 확인 (1000000 이상)
        userIds.all { it >= 1000000L } shouldBe true

        // 최소한의 성공률 보장 (80% 이상)
        results.size shouldBe users.size // 모든 회원가입이 성공해야 함

        // 부수 효과 검증 (관대하게)
        fakeMailSender.getSentMailCount() shouldBe results.size

        // 캐시 저장 검증 (성공한 케이스들만)
        results.forEach { user ->
            val cacheKey = "registration_code:${user.email.value}"
            fakeCacheStore.contains(cacheKey) shouldBe true
        }
    }

    test("대량의 동시 회원가입에서도 ID 중복이 발생하지 않아야 한다") {
        // given
        val userCount = 50
        val users =
            (1..userCount).map {
                Triple(
                    Email("user$it@example.com"),
                    Nickname("user$it"),
                    RawPassword("Password$it!"),
                )
            }

        val latch = CountDownLatch(userCount)
        val exceptions = mutableListOf<Exception>()

        // when: 대량 동시 실행
        val futures =
            users.map { (email, nickname, password) ->
                CompletableFuture.supplyAsync {
                    try {
                        userSignUpFacade.handle(
                            email = email,
                            nickname = nickname,
                            password = password,
                            confirmPassword = password,
                        )
                        fakeUserRepository.findBy(email)?.id?.value
                    } catch (e: Exception) {
                        synchronized(exceptions) {
                            exceptions.add(e)
                        }
                        null
                    } finally {
                        latch.countDown()
                    }
                }
            }

        latch.await(15, TimeUnit.SECONDS)
        val generatedIds = futures.mapNotNull { it.get() }

        // then
        exceptions.size shouldBe 0
        generatedIds.size shouldBe userCount

        // ID 중복 검사: Set으로 변환했을 때 크기가 같아야 함
        generatedIds.toSet().size shouldBe userCount

        // ID 순서 검사: 연속된 값들이어야 함 (AtomicLong.getAndIncrement() 특성)
        val sortedIds = generatedIds.sorted()
        val expectedIds = (1000000L until 1000000L + userCount).toList()
        sortedIds shouldBe expectedIds
    }
})
