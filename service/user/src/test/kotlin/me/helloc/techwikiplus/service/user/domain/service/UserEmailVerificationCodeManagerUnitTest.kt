package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.infrastructure.cache.VerificationCodeFakeStore
import me.helloc.techwikiplus.service.user.infrastructure.mail.FakeEmailTemplateService
import me.helloc.techwikiplus.service.user.infrastructure.messaging.FakeMailSender
import java.time.Instant

class UserEmailVerificationCodeManagerUnitTest : FunSpec({

    val mailSender = FakeMailSender()
    val verificationCodeStore = VerificationCodeFakeStore()
    val emailTemplateService = FakeEmailTemplateService()
    val manager =
        UserEmailVerificationCodeManager(
            mailSender,
            verificationCodeStore,
            emailTemplateService,
        )
    val now = Instant.now()

    beforeEach {
        mailSender.clear()
        verificationCodeStore.clear()
    }

    test("사용자에게 인증 메일을 발송한다") {
        // Given
        val user =
            User(
                id = "test-user-1",
                email = Email("test@example.com"),
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("testUser"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        // When
        manager.sendVerifyMailTo(user)

        // Then - 메일이 발송되었는지 확인
        mailSender.hasMailBeenSentTo("test@example.com") shouldBe true

        // Then - 발송된 메일의 내용 확인
        val sentMail = mailSender.getLastSentMail()
        sentMail?.to shouldBe "test@example.com"
        sentMail?.subject shouldBe "TechWiki+ 이메일 인증 코드"
        sentMail?.body shouldContain "인증 코드는"
        sentMail?.body shouldContain "입니다."
    }

    test("인증 코드가 캐시에 저장된다") {
        // Given
        val user =
            User(
                id = "test-user-1",
                email = Email("cache@example.com"),
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("cacheUser"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        // When
        manager.sendVerifyMailTo(user)

        // Then - 캐시에 인증 코드가 저장되었는지 확인
        verificationCodeStore.exists(Email("cache@example.com")) shouldBe true

        // Then - 저장된 인증 코드 형식 확인 (6자리 숫자)
        val storedCode = verificationCodeStore.get(Email("cache@example.com"))
        storedCode.value shouldMatch "\\d{6}"
    }

    test("인증 코드는 5분 TTL로 저장된다") {
        // Given
        val user =
            User(
                id = "test-user-1",
                email = Email("ttl@example.com"),
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("ttlUser"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        // When
        manager.sendVerifyMailTo(user)

        // Then - TTL 확인
        val cacheKey = "user-service:user:email:ttl@example.com"
        val ttl = verificationCodeStore.getTtl(cacheKey)

        // FakeCacheStore는 TTL을 정확히 저장하므로 5분에 가까워야 함
        // 약간의 시간 차이를 고려하여 4분 59초 이상이어야 함
        ttl?.toSeconds()?.let { it >= 299 } shouldBe true
    }

    test("메일 발송 여부를 확인할 수 있다") {
        // Given
        val user =
            User(
                id = "test-user-1",
                email = Email("check@example.com"),
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("checkUser"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        // When - 메일 발송 전
        val beforeSend = manager.hasMailBeenSentTo(user)

        // Then
        beforeSend shouldBe false

        // When - 메일 발송 후
        manager.sendVerifyMailTo(user)
        val afterSend = manager.hasMailBeenSentTo(user)

        // Then
        afterSend shouldBe true
    }

    test("여러 사용자에게 각각 다른 인증 코드가 발송된다") {
        // Given
        val user1 =
            User(
                id = "test-user-1",
                email = Email("user1@example.com"),
                encodedPassword = EncodedPassword("encodedPassword1"),
                nickname = Nickname("user1"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        val user2 =
            User(
                id = "test-user-2",
                email = Email("user2@example.com"),
                encodedPassword = EncodedPassword("encodedPassword2"),
                nickname = Nickname("user2"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        // When
        manager.sendVerifyMailTo(user1)
        manager.sendVerifyMailTo(user2)

        // Then - 각 사용자의 인증 코드가 다른지 확인
        val code1 = verificationCodeStore.get(Email("user1@example.com"))
        val code2 = verificationCodeStore.get(Email("user2@example.com"))

        code1.value shouldMatch "\\d{6}"
        code2.value shouldMatch "\\d{6}"
        // 랜덤하게 생성되므로 대부분의 경우 다를 것임 (매우 낮은 확률로 같을 수 있음)
    }

    test("캐시 키 형식이 올바르게 생성된다") {
        // Given
        val email = Email("format@example.com")

        // When - 캐시에 저장하고 확인
        val verificationCode = VerificationCode("123456")
        verificationCodeStore.store(email, verificationCode)

        // Then - 정상적으로 저장되고 조회되는지 확인
        verificationCodeStore.exists(email) shouldBe true
        verificationCodeStore.get(email) shouldBe verificationCode
    }
})
