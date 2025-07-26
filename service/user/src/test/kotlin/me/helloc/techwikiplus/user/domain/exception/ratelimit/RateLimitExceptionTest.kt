package me.helloc.techwikiplus.user.domain.exception.ratelimit

import me.helloc.techwikiplus.user.domain.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RateLimitExceptionTest {
    @Test
    fun `should create ResendRateLimitExceededException with correct error code and message`() {
        // given
        val details = "You can resend verification email after 5 minutes"

        // when
        val exception = ResendRateLimitExceededException(details)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED)
        assertThat(exception.code).isEqualTo("RATE_001")
        assertThat(
            exception.message,
        ).isEqualTo("Rate limit exceeded. Details: You can resend verification email after 5 minutes")
    }
}
