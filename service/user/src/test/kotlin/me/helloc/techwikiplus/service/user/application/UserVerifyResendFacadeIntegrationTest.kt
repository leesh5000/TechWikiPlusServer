package me.helloc.techwikiplus.service.user.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotPendingException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.service.UserEmailVerificationCodeManager
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.infrastructure.cache.VerificationCodeFakeStore
import me.helloc.techwikiplus.service.user.infrastructure.mail.FakeEmailTemplateService
import me.helloc.techwikiplus.service.user.infrastructure.messaging.FakeMailSender
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserVerifyResendUseCase
import java.time.Instant

class UserVerifyResendFacadeIntegrationTest : FunSpec({

    test("PENDING 상태의 사용자에게 인증 메일을 재전송하는 통합 테스트") {
        // Given
        val now = Instant.now()
        val repository = FakeUserRepository()
        val mailSender = FakeMailSender()
        val verificationCodeStore = VerificationCodeFakeStore()

        // 도메인 서비스 구성
        val userReader = UserReader(repository)
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                verificationCodeStore,
                emailTemplateService,
            )

        // Facade 구성
        val sut =
            UserVerifyResendFacade(
                userReader = userReader,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // PENDING 사용자 생성
        val email = Email("pending@example.com")
        val pendingUser =
            User(
                id = "test-user-1",
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("pendingUser"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(pendingUser)

        // When
        sut.execute(UserVerifyResendUseCase.Command(email))

        // Then
        // 메일이 발송되었는지 확인
        mailSender.hasMailBeenSentTo(email.value) shouldBe true

        // 발송된 메일 내용 확인
        val sentMail = mailSender.getLastSentMail()
        sentMail?.to shouldBe email.value
        sentMail?.subject shouldBe "TechWiki+ 이메일 인증 코드"
        sentMail?.body?.contains("인증 코드는") shouldBe true

        // 캐시에 인증 코드가 저장되었는지 확인
        verificationCodeStore.exists(email) shouldBe true

        // 저장된 인증 코드가 6자리인지 확인
        val storedCode = verificationCodeStore.get(email)
        storedCode.value.length shouldBe 6
    }

    test("ACTIVE 상태의 사용자에게 재전송 시도 시 예외 발생 통합 테스트") {
        // Given
        val now = Instant.now()
        val repository = FakeUserRepository()
        val mailSender = FakeMailSender()
        val verificationCodeStore = VerificationCodeFakeStore()

        // 도메인 서비스 구성
        val userReader = UserReader(repository)
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                verificationCodeStore,
                emailTemplateService,
            )

        // Facade 구성
        val sut =
            UserVerifyResendFacade(
                userReader = userReader,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        // ACTIVE 사용자 생성
        val email = Email("active@example.com")
        val activeUser =
            User(
                id = "test-user-2",
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("activeUser"),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(activeUser)

        // When & Then
        val exception =
            shouldThrow<UserNotPendingException> {
                sut.execute(UserVerifyResendUseCase.Command(email))
            }
        exception.message shouldBe "User with email active@example.com is not in pending status"

        // 메일이 발송되지 않았는지 확인
        mailSender.hasMailBeenSentTo(email.value) shouldBe false
        mailSender.getSentMails().size shouldBe 0
    }

    test("존재하지 않는 사용자에게 재전송 시도 시 예외 발생 통합 테스트") {
        // Given
        val repository = FakeUserRepository()
        val mailSender = FakeMailSender()
        val verificationCodeStore = VerificationCodeFakeStore()

        // 도메인 서비스 구성
        val userReader = UserReader(repository)
        val emailTemplateService = FakeEmailTemplateService()
        val userEmailVerificationCodeManager =
            UserEmailVerificationCodeManager(
                mailSender,
                verificationCodeStore,
                emailTemplateService,
            )

        // Facade 구성
        val sut =
            UserVerifyResendFacade(
                userReader = userReader,
                userEmailVerificationCodeManager = userEmailVerificationCodeManager,
            )

        val email = Email("nonexistent@example.com")

        // When & Then
        val exception =
            shouldThrow<UserNotFoundException> {
                sut.execute(UserVerifyResendUseCase.Command(email))
            }
        exception.message shouldBe "User not found: User with email nonexistent@example.com not found"

        // 메일이 발송되지 않았는지 확인
        mailSender.hasMailBeenSentTo(email.value) shouldBe false
        mailSender.getSentMails().size shouldBe 0
    }
})
