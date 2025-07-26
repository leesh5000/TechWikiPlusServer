package me.helloc.techwikiplus.user.domain.exception.validation

import me.helloc.techwikiplus.user.domain.DomainConstants
import me.helloc.techwikiplus.user.domain.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ValidationExceptionTest {
    @Test
    fun `should create InvalidEmailException with correct error code and message`() {
        // given
        val email = "invalid.email"

        // when
        val exception = InvalidEmailException(email)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_EMAIL)
        assertThat(exception.code).isEqualTo("USER_001")
        assertThat(exception.message).isEqualTo("Invalid email format. Details: Your input: invalid.email")
    }

    @Test
    fun `should create InvalidNicknameException with correct error code and message`() {
        // given
        val nickname = "ab"

        // when
        val exception = InvalidNicknameException(nickname)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_NICKNAME)
        assertThat(exception.code).isEqualTo("USER_002")
        assertThat(exception.message).contains("Invalid nickname format")
        assertThat(exception.message).contains("Your input: ab")
        assertThat(
            exception.message,
        ).contains("${DomainConstants.Nickname.MIN_LENGTH}-${DomainConstants.Nickname.MAX_LENGTH}")
    }

    @Test
    fun `should create InvalidPasswordException with correct error code and message`() {
        // given
        val password = "weak"

        // when
        val exception = InvalidPasswordException(password)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_PASSWORD)
        assertThat(exception.code).isEqualTo("USER_003")
        assertThat(exception.message).contains("Invalid password format")
        assertThat(exception.message).contains("Your input: weak")
        assertThat(
            exception.message,
        ).contains("${DomainConstants.Password.MIN_LENGTH}-${DomainConstants.Password.MAX_LENGTH}")
    }

    @Test
    fun `should create AlreadyVerifiedEmailException with correct error code and message`() {
        // given
        val email = "test@example.com"

        // when
        val exception = AlreadyVerifiedEmailException(email)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.ALREADY_VERIFIED_EMAIL)
        assertThat(exception.code).isEqualTo("USER_004")
        assertThat(exception.message).isEqualTo("Email is already verified. Details: Your input: test@example.com")
    }
}
