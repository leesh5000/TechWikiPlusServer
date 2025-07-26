package me.helloc.techwikiplus.user.interfaces.http

import jakarta.servlet.http.HttpServletRequest
import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidCredentialsException
import me.helloc.techwikiplus.user.domain.exception.conflict.DuplicateEmailException
import me.helloc.techwikiplus.user.domain.exception.notfound.UserEmailNotFoundException
import me.helloc.techwikiplus.user.domain.exception.ratelimit.ResendRateLimitExceededException
import me.helloc.techwikiplus.user.domain.exception.validation.InvalidEmailException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
    @DisplayName("유효성 검증 예외가 400 상태 코드로 처리되어야 한다")
    fun shouldHandleValidationExceptionWith400Status() {
        // given
        val exception = InvalidEmailException("invalid-email")

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.errorCode).isEqualTo("USER_001")
        assertThat(response.body!!.message).contains("Invalid email format")
        assertThat(response.body!!.path).isEqualTo("/api/v1/users/test")
        assertThat(response.body!!.timestamp).isNotEmpty()
    }

    @Test
    @DisplayName("인증 예외가 401 상태 코드로 처리되어야 한다")
    fun shouldHandleAuthenticationExceptionWith401Status() {
        // given
        val exception = InvalidCredentialsException()

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body!!.errorCode).isEqualTo("AUTH_004")
        assertThat(response.body!!.message).isEqualTo("Invalid email or password")
    }

    @Test
    @DisplayName("리소스를 찾을 수 없는 예외가 404 상태 코드로 처리되어야 한다")
    fun shouldHandleNotFoundExceptionWith404Status() {
        // given
        val exception = UserEmailNotFoundException("notfound@example.com")

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body!!.errorCode).isEqualTo("USER_101")
        assertThat(response.body!!.message).contains("User not found")
    }

    @Test
    @DisplayName("중복 리소스 예외가 409 상태 코드로 처리되어야 한다")
    fun shouldHandleConflictExceptionWith409Status() {
        // given
        val exception = DuplicateEmailException("duplicate@example.com")

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body!!.errorCode).isEqualTo("USER_201")
        assertThat(response.body!!.message).contains("Email already exists")
    }

    @Test
    @DisplayName("Rate Limit 예외가 429 상태 코드로 처리되어야 한다")
    fun shouldHandleRateLimitExceptionWith429Status() {
        // given
        val exception = ResendRateLimitExceededException("You can resend after 5 minutes")

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        assertThat(response.body!!.errorCode).isEqualTo("RATE_001")
        assertThat(response.body!!.message).contains("Rate limit exceeded")
    }

    @Test
    @DisplayName("필수 파라미터 누락 예외가 400 상태 코드로 처리되어야 한다")
    fun shouldHandleMissingParameterExceptionWith400Status() {
        // given
        val exception = MissingServletRequestParameterException("email", "String")

        // when
        val response = handler.handleMissingServletRequestParameter(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body!!.errorCode).isEqualTo("MISSING_PARAMETER")
        assertThat(response.body!!.message).isEqualTo("Required parameter 'email' is missing")
    }

    @Test
    @DisplayName("일반 예외가 500 상태 코드로 처리되어야 한다")
    fun shouldHandleGenericExceptionWith500Status() {
        // given
        val exception = RuntimeException("Unexpected error")

        // when
        val response = handler.handleGenericException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body!!.errorCode).isEqualTo("INTERNAL_SERVER_ERROR")
        assertThat(response.body!!.message).isEqualTo("Unexpected error")
    }
}
