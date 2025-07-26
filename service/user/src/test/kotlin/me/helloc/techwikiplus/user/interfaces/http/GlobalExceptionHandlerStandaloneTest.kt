package me.helloc.techwikiplus.user.interfaces.http

import jakarta.servlet.http.HttpServletRequest
import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidCredentialsException
import me.helloc.techwikiplus.user.infrastructure.exception.DataAccessException
import me.helloc.techwikiplus.user.infrastructure.exception.InfrastructureException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.context.MessageSource
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus

/**
 * GlobalExceptionHandler의 새로운 기능들을 테스트하는 독립적인 테스트 클래스
 * 기존 테스트와의 충돌을 피하기 위해 별도로 작성
 */
class GlobalExceptionHandlerStandaloneTest {
    private lateinit var handler: GlobalExceptionHandler
    private lateinit var request: HttpServletRequest
    private lateinit var messageSource: MessageSource
    private lateinit var environment: Environment

    @BeforeEach
    fun setUp() {
        messageSource = mock(MessageSource::class.java)
        environment = mock(Environment::class.java)
        handler = GlobalExceptionHandler(messageSource, environment)

        request = mock(HttpServletRequest::class.java)
        `when`(request.requestURI).thenReturn("/api/v1/users/test")
    }

    @Test
    @DisplayName("InfrastructureException이 500 상태 코드로 처리되어야 한다")
    fun shouldHandleInfrastructureException() {
        // given
        val exception = InfrastructureException("Database connection failed")
        `when`(environment.activeProfiles).thenReturn(arrayOf("development"))

        // when
        val response = handler.handleInfrastructureException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.errorCode).isEqualTo("INFRA_ERROR")
        assertThat(response.body!!.message).contains("Database connection failed")
    }

    @Test
    @DisplayName("재시도 가능한 InfrastructureException은 503 상태와 Retry-After 헤더를 포함한다")
    fun shouldHandleRetryableException() {
        // given
        val exception = InfrastructureException("Service unavailable", retryable = true)
        `when`(environment.activeProfiles).thenReturn(arrayOf("development"))

        // when
        val response = handler.handleInfrastructureException(exception, request)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(response.headers.getFirst("Retry-After")).isEqualTo("60")
    }

    @Test
    @DisplayName("Production 환경에서는 민감한 정보가 숨겨진다")
    fun shouldHideSensitiveInfoInProduction() {
        // given
        val cause = RuntimeException("Password: secret123")
        val exception = DataAccessException("Database error", cause)
        `when`(environment.activeProfiles).thenReturn(arrayOf("production"))

        // when
        val response = handler.handleInfrastructureException(exception, request)

        // then
        assertThat(response.body!!.message).isEqualTo("An error occurred while processing your request")
        assertThat(response.body!!.details).isNull()
    }

    @Test
    @DisplayName("Development 환경에서는 상세 정보가 포함된다")
    fun shouldIncludeDetailsInDevelopment() {
        // given
        val cause = RuntimeException("Connection timeout")
        val exception = DataAccessException("Database error", cause)
        `when`(environment.activeProfiles).thenReturn(arrayOf("development"))

        // when
        val response = handler.handleInfrastructureException(exception, request)

        // then
        assertThat(response.body!!.message).contains("Database error")
        assertThat(response.body!!.details).isNotNull
        assertThat(response.body!!.details!!["cause"]).isEqualTo("Connection timeout")
    }

    @Test
    @DisplayName("DomainException에 localizedMessage가 포함된다")
    fun shouldIncludeLocalizedMessageForDomainException() {
        // given
        val exception = InvalidCredentialsException()
        `when`(request.getHeader("Accept-Language")).thenReturn("ko-KR")

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.body!!.localizedMessage).isNotNull
        // MessageSource가 mock이므로 기본 메시지가 반환됨
        assertThat(response.body!!.localizedMessage).isEqualTo("Auth 004")
    }
}
