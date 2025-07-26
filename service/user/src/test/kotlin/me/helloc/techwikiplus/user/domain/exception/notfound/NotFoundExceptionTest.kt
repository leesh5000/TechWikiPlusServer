package me.helloc.techwikiplus.user.domain.exception.notfound

import me.helloc.techwikiplus.user.domain.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NotFoundExceptionTest {
    @Test
    fun `should create UserEmailNotFoundException with correct error code and message`() {
        // given
        val email = "notfound@example.com"

        // when
        val exception = UserEmailNotFoundException(email)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_NOT_FOUND)
        assertThat(exception.code).isEqualTo("USER_101")
        assertThat(
            exception.message,
        ).isEqualTo("User not found. Details: User not found with email: notfound@example.com")
    }

    @Test
    fun `should create ResourceNotFoundException with correct error code and message`() {
        // given
        val resource = "Profile picture"

        // when
        val exception = ResourceNotFoundException(resource)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
        assertThat(exception.code).isEqualTo("USER_102")
        assertThat(exception.message).isEqualTo("Resource not found. Details: Resource not found: Profile picture")
    }
}
