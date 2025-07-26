package me.helloc.techwikiplus.user.domain.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DomainExceptionTest {
    @Test
    fun `should create domain exception with error code`() {
        // given
        val errorCode = ErrorCode.INVALID_EMAIL
        val details = "test@invalid"

        // when
        val exception = TestDomainException(errorCode, details)

        // then
        assertThat(exception.errorCode).isEqualTo(errorCode)
        assertThat(exception.message).isEqualTo("Invalid email format. Details: test@invalid")
        assertThat(exception.code).isEqualTo("USER_001")
    }

    @Test
    fun `should create domain exception without details`() {
        // given
        val errorCode = ErrorCode.USER_NOT_FOUND

        // when
        val exception = TestDomainException(errorCode)

        // then
        assertThat(exception.errorCode).isEqualTo(errorCode)
        assertThat(exception.message).isEqualTo("User not found")
        assertThat(exception.code).isEqualTo("USER_101")
    }

    // Test implementation for abstract class
    private class TestDomainException(
        errorCode: ErrorCode,
        details: String? = null,
    ) : DomainException(errorCode, details)
}
