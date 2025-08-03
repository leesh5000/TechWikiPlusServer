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
import me.helloc.techwikiplus.service.user.infrastructure.messaging.FakeMailSender
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import java.time.Instant

class UserVerifyResendFacadeTest : FunSpec({

    val now = Instant.now()
    val repository = FakeUserRepository()
    val mailSender = FakeMailSender()
    val verificationCodeStore = VerificationCodeFakeStore()
    val userReader = UserReader(repository)
    val userEmailVerificationCodeManager = UserEmailVerificationCodeManager(mailSender, verificationCodeStore)

    val sut =
        UserVerifyResendFacade(
            userReader = userReader,
            userEmailVerificationCodeManager = userEmailVerificationCodeManager,
        )

    beforeEach {
        repository.clear()
        mailSender.clear()
        verificationCodeStore.clear()
    }

    test("PENDING 상태의 사용자에게 인증 메일을 재전송한다") {
        // Given
        val email = Email("pending@example.com")
        val pendingUser =
            User(
                id = "user-1",
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
        sut.resend(email)

        // Then
        mailSender.hasMailBeenSentTo(email.value) shouldBe true
        verificationCodeStore.exists(email) shouldBe true
    }

    test("ACTIVE 상태의 사용자에게 재전송 시도 시 UserNotPendingException 발생") {
        // Given
        val email = Email("active@example.com")
        val activeUser =
            User(
                id = "user-2",
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
                sut.resend(email)
            }
        exception.message shouldBe "User with email active@example.com is not in pending status"

        // 메일이 발송되지 않았는지 확인
        mailSender.hasMailBeenSentTo(email.value) shouldBe false
    }

    test("DORMANT 상태의 사용자에게 재전송 시도 시 UserNotPendingException 발생") {
        // Given
        val email = Email("dormant@example.com")
        val dormantUser =
            User(
                id = "user-3",
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("dormantUser"),
                status = UserStatus.DORMANT,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(dormantUser)

        // When & Then
        shouldThrow<UserNotPendingException> {
            sut.resend(email)
        }

        // 메일이 발송되지 않았는지 확인
        mailSender.hasMailBeenSentTo(email.value) shouldBe false
    }

    test("BANNED 상태의 사용자에게 재전송 시도 시 UserNotPendingException 발생") {
        // Given
        val email = Email("banned@example.com")
        val bannedUser =
            User(
                id = "user-4",
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("bannedUser"),
                status = UserStatus.BANNED,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(bannedUser)

        // When & Then
        shouldThrow<UserNotPendingException> {
            sut.resend(email)
        }

        // 메일이 발송되지 않았는지 확인
        mailSender.hasMailBeenSentTo(email.value) shouldBe false
    }

    test("DELETED 상태의 사용자에게 재전송 시도 시 UserNotPendingException 발생") {
        // Given
        val email = Email("deleted@example.com")
        val deletedUser =
            User(
                id = "user-5",
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("deletedUser"),
                status = UserStatus.DELETED,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(deletedUser)

        // When & Then
        shouldThrow<UserNotPendingException> {
            sut.resend(email)
        }

        // 메일이 발송되지 않았는지 확인
        mailSender.hasMailBeenSentTo(email.value) shouldBe false
    }

    test("존재하지 않는 사용자에게 재전송 시도 시 UserNotFoundException 발생") {
        // Given
        val email = Email("nonexistent@example.com")

        // When & Then
        val exception =
            shouldThrow<UserNotFoundException> {
                sut.resend(email)
            }
        exception.message shouldBe "User not found: User with email nonexistent@example.com not found"

        // 메일이 발송되지 않았는지 확인
        mailSender.hasMailBeenSentTo(email.value) shouldBe false
    }

    test("PENDING 사용자에게 여러 번 재전송 가능") {
        // Given
        val email = Email("multi-resend@example.com")
        val pendingUser =
            User(
                id = "user-6",
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("multiResendUser"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        repository.save(pendingUser)

        // When
        sut.resend(email)
        sut.resend(email)
        sut.resend(email)

        // Then
        mailSender.getSentMails().size shouldBe 3
        mailSender.hasMailBeenSentTo(email.value) shouldBe true
    }
})
