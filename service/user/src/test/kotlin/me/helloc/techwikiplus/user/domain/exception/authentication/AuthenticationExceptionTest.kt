package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AuthenticationExceptionTest {
    @Test
    fun `should create ExpiredEmailVerificationException with correct error code and message`() {
        // given
        val email = "expired@example.com"

        // when
        val exception = ExpiredEmailVerificationException(email)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.EXPIRED_EMAIL_VERIFICATION)
        assertThat(exception.code).isEqualTo("AUTH_001")
        assertThat(exception.message).contains("Email verification expired")
        assertThat(exception.message).contains(email)
    }

    @Test
    fun `should create InvalidCredentialsException with correct error code and message`() {
        // when
        val exception = InvalidCredentialsException()

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_CREDENTIALS)
        assertThat(exception.code).isEqualTo("AUTH_004")
        assertThat(exception.message).isEqualTo("Invalid email or password")
    }

    @Test
    fun `should create EmailNotVerifiedException with correct error code and message`() {
        // when
        val exception = EmailNotVerifiedException()

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED)
        assertThat(exception.code).isEqualTo("AUTH_006")
        assertThat(exception.message).contains("Email not verified")
    }

    @Test
    fun `should create InvalidTokenException with correct error code and message`() {
        // when
        val exception = InvalidTokenException()

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_TOKEN)
        assertThat(exception.code).isEqualTo("AUTH_007")
        assertThat(exception.message).contains("Invalid token")
    }

    @Test
    fun `should create AccountBannedException with correct error code and message`() {
        // when
        val exception = AccountBannedException()

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.ACCOUNT_BANNED)
        assertThat(exception.code).isEqualTo("AUTH_009")
        assertThat(exception.message).contains("Account has been banned")
    }
}
