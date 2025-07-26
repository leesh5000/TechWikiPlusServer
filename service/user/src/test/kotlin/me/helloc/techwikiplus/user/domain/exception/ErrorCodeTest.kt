package me.helloc.techwikiplus.user.domain.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ErrorCodeTest {
    @Test
    fun `should have correct error codes for validation errors`() {
        // given & when & then
        assertThat(ErrorCode.INVALID_EMAIL.code).isEqualTo("USER_001")
        assertThat(ErrorCode.INVALID_EMAIL.message).isEqualTo("Invalid email format")

        assertThat(ErrorCode.INVALID_NICKNAME.code).isEqualTo("USER_002")
        assertThat(ErrorCode.INVALID_NICKNAME.message).isEqualTo("Invalid nickname format")

        assertThat(ErrorCode.INVALID_PASSWORD.code).isEqualTo("USER_003")
        assertThat(ErrorCode.INVALID_PASSWORD.message).isEqualTo("Invalid password format")

        assertThat(ErrorCode.ALREADY_VERIFIED_EMAIL.code).isEqualTo("USER_004")
        assertThat(ErrorCode.ALREADY_VERIFIED_EMAIL.message).isEqualTo("Email is already verified")
    }

    @Test
    fun `should have correct error codes for not found errors`() {
        // given & when & then
        assertThat(ErrorCode.USER_NOT_FOUND.code).isEqualTo("USER_101")
        assertThat(ErrorCode.USER_NOT_FOUND.message).isEqualTo("User not found")

        assertThat(ErrorCode.RESOURCE_NOT_FOUND.code).isEqualTo("USER_102")
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.message).isEqualTo("Resource not found")
    }

    @Test
    fun `should have correct error codes for conflict errors`() {
        // given & when & then
        assertThat(ErrorCode.DUPLICATE_EMAIL.code).isEqualTo("USER_201")
        assertThat(ErrorCode.DUPLICATE_EMAIL.message).isEqualTo("Email already exists")

        assertThat(ErrorCode.DUPLICATE_NICKNAME.code).isEqualTo("USER_202")
        assertThat(ErrorCode.DUPLICATE_NICKNAME.message).isEqualTo("Nickname already exists")
    }

    @Test
    fun `should have correct error codes for authentication errors`() {
        // given & when & then
        assertThat(ErrorCode.EXPIRED_EMAIL_VERIFICATION.code).isEqualTo("AUTH_001")
        assertThat(ErrorCode.EXPIRED_EMAIL_VERIFICATION.message).isEqualTo("Email verification expired")

        assertThat(ErrorCode.PENDING_USER_NOT_FOUND.code).isEqualTo("AUTH_002")
        assertThat(ErrorCode.PENDING_USER_NOT_FOUND.message).isEqualTo("Pending user not found")

        assertThat(ErrorCode.INVALID_VERIFICATION_CODE.code).isEqualTo("AUTH_003")
        assertThat(ErrorCode.INVALID_VERIFICATION_CODE.message).isEqualTo("Invalid verification code")

        assertThat(ErrorCode.INVALID_CREDENTIALS.code).isEqualTo("AUTH_004")
        assertThat(ErrorCode.INVALID_CREDENTIALS.message).isEqualTo("Invalid email or password")

        assertThat(ErrorCode.UNAUTHORIZED_ACCESS.code).isEqualTo("AUTH_005")
        assertThat(ErrorCode.UNAUTHORIZED_ACCESS.message).isEqualTo("Unauthorized access")

        assertThat(ErrorCode.EMAIL_NOT_VERIFIED.code).isEqualTo("AUTH_006")
        assertThat(ErrorCode.EMAIL_NOT_VERIFIED.message).isEqualTo("Email not verified")

        assertThat(ErrorCode.INVALID_TOKEN.code).isEqualTo("AUTH_007")
        assertThat(ErrorCode.INVALID_TOKEN.message).isEqualTo("Invalid token")

        assertThat(ErrorCode.INVALID_TOKEN_TYPE.code).isEqualTo("AUTH_008")
        assertThat(ErrorCode.INVALID_TOKEN_TYPE.message).isEqualTo("Invalid token type")

        assertThat(ErrorCode.ACCOUNT_BANNED.code).isEqualTo("AUTH_009")
        assertThat(ErrorCode.ACCOUNT_BANNED.message).isEqualTo("Account has been banned")

        assertThat(ErrorCode.ACCOUNT_DORMANT.code).isEqualTo("AUTH_010")
        assertThat(ErrorCode.ACCOUNT_DORMANT.message).isEqualTo("Account is dormant")

        assertThat(ErrorCode.ACCOUNT_DELETED.code).isEqualTo("AUTH_011")
        assertThat(ErrorCode.ACCOUNT_DELETED.message).isEqualTo("Account has been deleted")
    }

    @Test
    fun `should have correct error codes for rate limit errors`() {
        // given & when & then
        assertThat(ErrorCode.RATE_LIMIT_EXCEEDED.code).isEqualTo("RATE_001")
        assertThat(ErrorCode.RATE_LIMIT_EXCEEDED.message).isEqualTo("Rate limit exceeded")
    }
}
