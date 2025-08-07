package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.FakeCacheStore
import me.helloc.techwikiplus.service.user.domain.port.FakeMailSender
import java.time.Duration
import java.time.Instant

class EmailVerifyServiceTest : FunSpec({

    lateinit var emailVerifyService: EmailVerifyService
    lateinit var fakeMailSender: FakeMailSender
    lateinit var fakeCacheStore: FakeCacheStore

    beforeEach {
        fakeMailSender = FakeMailSender()
        fakeCacheStore = FakeCacheStore()

        emailVerifyService =
            EmailVerifyService(
                mailSender = fakeMailSender,
                cacheStore = fakeCacheStore,
            )
    }

    test("startVerification 호출 시 메일이 발송되어야 한다") {
        // given
        val user =
            User.create(
                id = UserId("test-user-1"),
                email = Email("test@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("encoded_password"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
            )

        // when
        emailVerifyService.startVerification(user)

        // then
        fakeMailSender.getSentMailCount() shouldBe 1
        fakeMailSender.wasSentTo(user.email) shouldBe true

        val sentMail = fakeMailSender.getLastSentMail()
        sentMail shouldNotBe null
        sentMail!!.to shouldBe user.email
        sentMail.content.subject shouldBe "TechWiki+ 회원가입 인증 코드"
        sentMail.content.body shouldNotBe null
    }

    test("startVerification 호출 시 인증 코드가 캐시에 저장되어야 한다") {
        // given
        val user =
            User.create(
                id = UserId("test-user-1"),
                email = Email("test@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("encoded_password"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
            )

        // when
        emailVerifyService.startVerification(user)

        // then
        val cacheKey = "registration_code::${user.email}"
        fakeCacheStore.contains(cacheKey) shouldBe true

        val storedCode = fakeCacheStore.get(cacheKey)
        storedCode shouldNotBe null
        storedCode!!.length shouldBe 6
        storedCode.all { it.isDigit() } shouldBe true
    }

    test("startVerification 호출 시 캐시에 저장된 코드가 5분 후 만료되어야 한다") {
        // given
        val user =
            User.create(
                id = UserId("test-user-1"),
                email = Email("test@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("encoded_password"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
            )

        // when
        emailVerifyService.startVerification(user)

        // then
        val cacheKey = "registration_code::${user.email}"
        fakeCacheStore.contains(cacheKey) shouldBe true

        // 5분 후
        fakeCacheStore.advanceTimeBy(Duration.ofMinutes(5).plusSeconds(1))
        fakeCacheStore.contains(cacheKey) shouldBe false
    }

    test("올바른 인증 코드로 verify 호출 시 검증이 성공해야 한다") {
        // given
        val email = Email("test@example.com")
        val registrationCode = RegistrationCode("123456")
        val cacheKey = "registration_code::$email"
        fakeCacheStore.put(cacheKey, registrationCode.value, Duration.ofMinutes(5))

        // when
        emailVerifyService.verify(email, registrationCode)

        // then
        fakeCacheStore.contains(cacheKey) shouldBe false
    }

    test("잘못된 인증 코드로 verify 호출 시 CODE_MISMATCH 예외가 발생해야 한다") {
        // given
        val email = Email("test@example.com")
        val correctCode = RegistrationCode("123456")
        val wrongCode = RegistrationCode("654321")
        val cacheKey = "registration_code::$email"
        fakeCacheStore.put(cacheKey, correctCode.value, Duration.ofMinutes(5))

        // when & then
        val exception =
            shouldThrow<DomainException> {
                emailVerifyService.verify(email, wrongCode)
            }

        exception.errorCode shouldBe ErrorCode.CODE_MISMATCH
        fakeCacheStore.contains(cacheKey) shouldBe true
    }

    test("캐시에 인증 코드가 없을 때 verify 호출 시 REGISTRATION_EXPIRED 예외가 발생해야 한다") {
        // given
        val email = Email("test@example.com")
        val registrationCode = RegistrationCode("123456")

        // when & then
        val exception =
            shouldThrow<DomainException> {
                emailVerifyService.verify(email, registrationCode)
            }

        exception.errorCode shouldBe ErrorCode.REGISTRATION_EXPIRED
        exception.params shouldBe arrayOf(email.value)
    }

    test("만료된 인증 코드로 verify 호출 시 REGISTRATION_EXPIRED 예외가 발생해야 한다") {
        // given
        val email = Email("test@example.com")
        val registrationCode = RegistrationCode("123456")
        val cacheKey = "registration_code::$email"
        fakeCacheStore.put(cacheKey, registrationCode.value, Duration.ofMinutes(5))

        // 5분 이상 시간 경과
        fakeCacheStore.advanceTimeBy(Duration.ofMinutes(5).plusSeconds(1))

        // when & then
        val exception =
            shouldThrow<DomainException> {
                emailVerifyService.verify(email, registrationCode)
            }

        exception.errorCode shouldBe ErrorCode.REGISTRATION_EXPIRED
        exception.params shouldBe arrayOf(email.value)
    }

    test("여러 사용자에 대한 인증 코드가 독립적으로 관리되어야 한다") {
        // given
        val user1 =
            User.create(
                id = UserId("user-1"),
                email = Email("user1@example.com"),
                nickname = Nickname("user1"),
                encodedPassword = EncodedPassword("encoded_password1"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
            )

        val user2 =
            User.create(
                id = UserId("user-2"),
                email = Email("user2@example.com"),
                nickname = Nickname("user2"),
                encodedPassword = EncodedPassword("encoded_password2"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
            )

        // when
        emailVerifyService.startVerification(user1)
        emailVerifyService.startVerification(user2)

        // then
        val cacheKey1 = "registration_code::${user1.email}"
        val cacheKey2 = "registration_code::${user2.email}"

        fakeCacheStore.contains(cacheKey1) shouldBe true
        fakeCacheStore.contains(cacheKey2) shouldBe true

        val code1 = fakeCacheStore.get(cacheKey1)
        val code2 = fakeCacheStore.get(cacheKey2)

        code1 shouldNotBe null
        code2 shouldNotBe null
        code1 shouldNotBe code2

        fakeMailSender.getSentMailCount() shouldBe 2
        fakeMailSender.wasSentTo(user1.email) shouldBe true
        fakeMailSender.wasSentTo(user2.email) shouldBe true
    }

    test("동일한 사용자가 여러 번 인증 코드를 요청하면 새로운 코드로 덮어써져야 한다") {
        // given
        val user =
            User.create(
                id = UserId("test-user-1"),
                email = Email("test@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("encoded_password"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
            )

        // when
        emailVerifyService.startVerification(user)
        val firstCode = fakeCacheStore.get("registration_code::${user.email}")

        emailVerifyService.startVerification(user)
        val secondCode = fakeCacheStore.get("registration_code::${user.email}")

        // then
        firstCode shouldNotBe null
        secondCode shouldNotBe null
        firstCode shouldNotBe secondCode

        fakeMailSender.getSentMailCount() shouldBe 2
        fakeMailSender.wasSentTo(user.email) shouldBe true
    }

    test("인증 성공 후 동일한 코드로 재인증 시도 시 REGISTRATION_EXPIRED 예외가 발생해야 한다") {
        // given
        val email = Email("test@example.com")
        val registrationCode = RegistrationCode("123456")
        val cacheKey = "registration_code::$email"
        fakeCacheStore.put(cacheKey, registrationCode.value, Duration.ofMinutes(5))

        // when
        emailVerifyService.verify(email, registrationCode)

        // then
        val exception =
            shouldThrow<DomainException> {
                emailVerifyService.verify(email, registrationCode)
            }

        exception.errorCode shouldBe ErrorCode.REGISTRATION_EXPIRED
        exception.params shouldBe arrayOf(email.value)
    }
})
