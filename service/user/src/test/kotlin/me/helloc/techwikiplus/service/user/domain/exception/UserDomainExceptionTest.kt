package me.helloc.techwikiplus.service.user.domain.exception

import junit.framework.TestCase.assertTrue
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class UserDomainExceptionTest {
    @Test
    @DisplayName("UserDomainException은 RuntimeException을 상속한다")
    fun `UserDomainException extends RuntimeException`() {
        val exception = UserDomainException("test message")
        assertTrue(exception is RuntimeException)
    }

    @Test
    @DisplayName("UserDomainException은 원인 예외를 포함할 수 있다")
    fun `UserDomainException can include cause`() {
        val cause = IllegalArgumentException("cause")
        val exception = UserDomainException("test message", cause)

        assertEquals("test message", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    @DisplayName("DormantUserException은 이메일 정보를 포함한 메시지를 생성한다")
    fun `DormantUserException creates message with email`() {
        val email = Email("test@example.com")
        val exception = DormantUserException(email)

        assertTrue(exception is UserDomainException)
        assertEquals(
            "User with email test@example.com is dormant. Please contact the administrator.",
            exception.message,
        )
    }

    @Test
    @DisplayName("BannedUserException은 이메일 정보를 포함한 메시지를 생성한다")
    fun `BannedUserException creates message with email`() {
        val email = Email("banned@example.com")
        val exception = BannedUserException(email)

        assertTrue(exception is UserDomainException)
        assertEquals(
            "User with email banned@example.com is banned. Please contact the administrator.",
            exception.message,
        )
    }

    @Test
    @DisplayName("PendingUserException은 이메일 정보를 포함한 메시지를 생성한다")
    fun `PendingUserException creates message with email`() {
        val email = Email("pending@example.com")
        val exception = PendingUserException(email)

        assertTrue(exception is UserDomainException)
        assertEquals(
            "User with email pending@example.com is pending activation. Please check your email " +
                "for the verification code.",
            exception.message,
        )
    }

    @Test
    @DisplayName("DeletedUserException은 이메일 정보를 포함한 메시지를 생성한다")
    fun `DeletedUserException creates message with email`() {
        val email = Email("deleted@example.com")
        val exception = DeletedUserException(email)

        assertTrue(exception is UserDomainException)
        assertEquals(
            "User with email deleted@example.com has been deleted. Please contact the administrator.",
            exception.message,
        )
    }

    @Test
    @DisplayName("UserAlreadyExistsException은 이메일로 생성할 수 있다")
    fun `UserAlreadyExistsException can be created with email`() {
        val email = Email("exists@example.com")
        val exception = UserAlreadyExistsException(email)

        assertTrue(exception is UserDomainException)
        assertEquals("이메일 'exists@example.com'은(는) 이미 사용 중입니다.", exception.message)
    }

    @Test
    @DisplayName("UserAlreadyExistsException은 닉네임으로 생성할 수 있다")
    fun `UserAlreadyExistsException can be created with nickname`() {
        val nickname = Nickname("existingNick")
        val exception = UserAlreadyExistsException(nickname)

        assertTrue(exception is UserDomainException)
        assertEquals("닉네임 'existingNick'은(는) 이미 사용 중입니다.", exception.message)
    }

    @Test
    @DisplayName("PasswordsDoNotMatchException은 고정 메시지를 가진다")
    fun `PasswordsDoNotMatchException has fixed message`() {
        val exception = PasswordsDoNotMatchException()

        assertTrue(exception is UserDomainException)
        assertEquals("비밀번호와 비밀번호 확인이 일치하지 않습니다.", exception.message)
    }

    @Test
    @DisplayName("PendingUserNotFoundException은 이메일 정보를 포함한 메시지를 생성한다")
    fun `PendingUserNotFoundException creates message with email`() {
        val email = Email("notfound@example.com")
        val exception = PendingUserNotFoundException(email)

        assertTrue(exception is UserDomainException)
        assertEquals(
            "회원 가입 대기중인 사용자(notfound@example.com)를 찾을 수 없습니다.",
            exception.message,
        )
    }

    @Test
    @DisplayName("ActiveUserNotFoundException은 UserId로 생성할 수 있다")
    fun `ActiveUserNotFoundException can be created with UserId`() {
        val userId = UserId("user123")
        val exception = ActiveUserNotFoundException(userId)

        assertTrue(exception is UserDomainException)
        assertEquals("활성화된 사용자(user123)를 찾을 수 없습니다.", exception.message)
    }

    @Test
    @DisplayName("ActiveUserNotFoundException은 Email로 생성할 수 있다")
    fun `ActiveUserNotFoundException can be created with Email`() {
        val email = Email("active@example.com")
        val exception = ActiveUserNotFoundException(email)

        assertTrue(exception is UserDomainException)
        assertEquals("활성화된 사용자(active@example.com)를 찾을 수 없습니다.", exception.message)
    }

    @Test
    @DisplayName("InvalidCredentialsException은 고정 메시지를 가진다")
    fun `InvalidCredentialsException has fixed message`() {
        val exception = InvalidCredentialsException()

        assertTrue(exception is UserDomainException)
        assertEquals("비밀번호가 일치하지 않습니다.", exception.message)
    }

    @Test
    @DisplayName("RegistrationEmailNotFoundException은 이메일 정보를 포함한 메시지를 생성한다")
    fun `RegistrationEmailNotFoundException creates message with email`() {
        val email = Email("registration@example.com")
        val exception = RegistrationEmailNotFoundException(email)

        assertTrue(exception is UserDomainException)
        assertTrue(exception.message!!.contains("registration@example.com"))
        assertTrue(exception.message!!.contains("회원 가입 코드가 존재하지 않습니다"))
    }

    @Test
    @DisplayName("RegistrationCodeMismatchException은 이메일 정보를 포함한 메시지를 생성한다")
    fun `RegistrationCodeMismatchException creates message with email`() {
        val email = Email("mismatch@example.com")
        val exception = RegistrationCodeMismatchException(email)

        assertTrue(exception is UserDomainException)
        assertTrue(exception.message!!.contains("mismatch@example.com"))
        assertTrue(exception.message!!.contains("회원 가입 코드가 일치하지 않습니다"))
    }

    @Test
    @DisplayName("MailSendException은 이메일과 원인 예외를 포함할 수 있다")
    fun `MailSendException can include email and cause`() {
        val email = Email("send@example.com")
        val cause = RuntimeException("mail server error")
        val exception = MailSendException(email, cause)

        assertTrue(exception is UserDomainException)
        assertEquals("이메일(send@example.com) 전송에 실패했습니다.", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    @DisplayName("UserNotificationFailedException은 다양한 생성자를 지원한다")
    fun `UserNotificationFailedException supports multiple constructors`() {
        val cause = RuntimeException("notification error")

        val exception1 = UserNotificationFailedException("custom message", cause)
        assertEquals("custom message", exception1.message)
        assertEquals(cause, exception1.cause)

        val exception2 = UserNotificationFailedException(cause)
        assertEquals("사용자 알림에 실패했습니다.", exception2.message)
        assertEquals(cause, exception2.cause)

        val exception3 = UserNotificationFailedException("message only")
        assertEquals("message only", exception3.message)
        assertNull(exception3.cause)
    }

    @Test
    @DisplayName("모든 도메인 예외는 catch 블록에서 UserDomainException으로 처리할 수 있다")
    fun `All domain exceptions can be caught as UserDomainException`() {
        val exceptions =
            listOf(
                DormantUserException(Email("test@example.com")),
                BannedUserException(Email("test@example.com")),
                PendingUserException(Email("test@example.com")),
                DeletedUserException(Email("test@example.com")),
                UserAlreadyExistsException(Email("test@example.com")),
                PasswordsDoNotMatchException(),
                PendingUserNotFoundException(Email("test@example.com")),
                ActiveUserNotFoundException(Email("test@example.com")),
                InvalidCredentialsException(),
                RegistrationEmailNotFoundException(Email("test@example.com")),
                RegistrationCodeMismatchException(Email("test@example.com")),
                MailSendException(Email("test@example.com")),
                UserNotificationFailedException("test"),
            )

        exceptions.forEach { exception ->
            assertThrows<UserDomainException> {
                throw exception
            }
        }
    }
}
