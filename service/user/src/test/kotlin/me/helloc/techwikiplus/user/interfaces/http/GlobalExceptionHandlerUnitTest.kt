package me.helloc.techwikiplus.user.interfaces.http

import jakarta.servlet.http.HttpServletRequest
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MissingServletRequestParameterException

class GlobalExceptionHandlerUnitTest {
    private lateinit var handler: GlobalExceptionHandler
    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
        request = mock(HttpServletRequest::class.java)
        `when`(request.requestURI).thenReturn("/api/v1/users/test")
    }

    @Test
    fun shouldHandleValidationExceptionWith400Status() {
        // given
        val exception = CustomException.ValidationException.InvalidEmail("invalid-email")

        // when
        val response = handler.handleCustomException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.errorCode).isEqualTo("VALIDATION_FAILED")
        assertThat(response.body!!.message).contains("Invalid email format")
        assertThat(response.body!!.path).isEqualTo("/api/v1/users/test")
        assertThat(response.body!!.timestamp).isNotEmpty()
    }

    @Test
    fun shouldHandleAuthenticationExceptionWith401Status() {
        // given
        val exception = CustomException.AuthenticationException.InvalidCredentials()

        // when
        val response = handler.handleCustomException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body!!.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(response.body!!.message).isEqualTo("Invalid email or password")
    }

    @Test
    fun shouldHandleNotFoundExceptionWith404Status() {
        // given
        val exception = CustomException.NotFoundException.UserEmailNotFoundException("test@example.com")

        // when
        val response = handler.handleCustomException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body!!.errorCode).isEqualTo("NOT_FOUND")
        assertThat(response.body!!.message).contains("User not found with email")
    }

    @Test
    fun shouldHandleConflictExceptionWith409Status() {
        // given
        val exception = CustomException.ConflictException.DuplicateEmail("duplicate@example.com")

        // when
        val response = handler.handleCustomException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body!!.errorCode).isEqualTo("CONFLICT")
        assertThat(response.body!!.message).contains("Email already exists")
    }

    @Test
    fun shouldHandleRateLimitExceptionWith429Status() {
        // given
        val exception = CustomException.ResendRateLimitExceeded("Too many requests")

        // when
        val response = handler.handleCustomException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        assertThat(response.body!!.errorCode).isEqualTo("RATE_LIMIT_EXCEEDED")
        assertThat(response.body!!.message).isEqualTo("Too many requests")
    }

    @Test
    fun shouldHandleGenericExceptionWith500Status() {
        // given
        val exception = RuntimeException("Unexpected error")

        // when
        val response = handler.handleGenericException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.errorCode).isEqualTo("INTERNAL_SERVER_ERROR")
        assertThat(response.body!!.message).isEqualTo("An unexpected error occurred.")
        assertThat(response.body!!.path).isEqualTo("/api/v1/users/test")
    }

    @Test
    fun shouldIncludeTimestampInISOFormat() {
        // given
        val exception = CustomException.ValidationException.InvalidPassword("weak")

        // when
        val response = handler.handleCustomException(exception, request)

        // then
        assertThat(response.body!!.timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
    }

    @Test
    fun shouldHandleDifferentRequestPaths() {
        // given
        val exception = CustomException.ConflictException.DuplicateNickname("existinguser")
        `when`(request.requestURI).thenReturn("/api/v1/users/signup")

        // when
        val response = handler.handleCustomException(exception, request)

        // then
        assertThat(response.body!!.path).isEqualTo("/api/v1/users/signup")
    }

    @Test
    fun shouldHandleAllAuthenticationExceptionTypes() {
        // given
        val exceptions =
            listOf(
                CustomException.AuthenticationException.EmailNotVerified(),
                CustomException.AuthenticationException.AccountBanned(),
                CustomException.AuthenticationException.AccountDormant(),
                CustomException.AuthenticationException.AccountDeleted(),
                CustomException.AuthenticationException.UnauthorizedAccess("resource"),
            )

        // when & then
        exceptions.forEach { exception ->
            val response = handler.handleCustomException(exception, request)
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
            assertThat(response.body!!.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        }
    }

    @Test
    fun shouldHandleNullExceptionMessage() {
        // given
        val exception =
            object : Exception() {
                override val message: String? = null
            }

        // when
        val response = handler.handleGenericException(exception, request)

        // then
        assertThat(response.body!!.message).isEqualTo("An unexpected error occurred.")
    }

    @Test
    fun shouldHandleMissingServletRequestParameterException() {
        // given
        val exception = MissingServletRequestParameterException("email", "String")

        // when
        val response = handler.handleMissingServletRequestParameter(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body!!.errorCode).isEqualTo("MISSING_PARAMETER")
        assertThat(response.body!!.message).isEqualTo("Required parameter 'email' is missing")
        assertThat(response.body!!.path).isEqualTo("/api/v1/users/test")
    }
}
