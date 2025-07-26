package me.helloc.techwikiplus.user.interfaces.http

import jakarta.servlet.http.HttpServletRequest
import me.helloc.techwikiplus.user.domain.exception.validation.InvalidEmailException
import me.helloc.techwikiplus.user.infrastructure.exception.DataAccessException
import me.helloc.techwikiplus.user.infrastructure.exception.ExternalServiceException
import me.helloc.techwikiplus.user.infrastructure.exception.MailDeliveryException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.context.MessageSource
import org.springframework.core.env.Environment
import java.util.Locale

class GlobalExceptionHandlerTest {
    private lateinit var messageSource: MessageSource
    private lateinit var environment: Environment
    private lateinit var handler: GlobalExceptionHandler
    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setUp() {
        messageSource = mock(MessageSource::class.java)
        environment = mock(Environment::class.java)
        handler = GlobalExceptionHandler(messageSource, environment)
        request = mock(HttpServletRequest::class.java)
        `when`(request.requestURI).thenReturn("/api/test")
    }

    @Test
    fun `handleDomainException should return localized message for Korean locale`() {
        // given
        val exception = InvalidEmailException("test@invalid")
        `when`(request.getHeader("Accept-Language")).thenReturn("ko-KR")
        `when`(
            messageSource.getMessage(
                "error.user.001",
                null,
                "User 001",
                Locale.KOREA,
            ),
        ).thenReturn("올바르지 않은 이메일 형식입니다")

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.statusCodeValue).isEqualTo(400)
        assertThat(response.body?.errorCode).isEqualTo("USER_001")
        assertThat(response.body?.localizedMessage).isEqualTo("올바르지 않은 이메일 형식입니다")
    }

    @Test
    fun `handleDomainException should return English message for English locale`() {
        // given
        val exception = InvalidEmailException("test@invalid")
        `when`(request.getHeader("Accept-Language")).thenReturn("en-US")
        `when`(
            messageSource.getMessage(
                "error.user.001",
                null,
                "User 001",
                Locale.ENGLISH,
            ),
        ).thenReturn("Invalid email format")

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.statusCodeValue).isEqualTo(400)
        assertThat(response.body?.errorCode).isEqualTo("USER_001")
        assertThat(response.body?.localizedMessage).isEqualTo("Invalid email format")
    }

    @Test
    fun `handleInfrastructureException should hide details in production`() {
        // given
        `when`(environment.activeProfiles).thenReturn(arrayOf("production"))
        val exception = DataAccessException("Database connection failed", RuntimeException("Connection refused"))

        // when
        val response = handler.handleInfrastructureException(exception, request)

        // then
        assertThat(response.statusCodeValue).isEqualTo(500)
        assertThat(response.body?.message).isEqualTo("An error occurred while processing your request")
        assertThat(response.body?.details).isNull()
    }

    @Test
    fun `handleInfrastructureException should show details in development`() {
        // given
        `when`(environment.activeProfiles).thenReturn(arrayOf("dev"))
        val cause = RuntimeException("Connection refused")
        val exception = DataAccessException("Database connection failed", cause)

        // when
        val response = handler.handleInfrastructureException(exception, request)

        // then
        assertThat(response.statusCodeValue).isEqualTo(500)
        assertThat(response.body?.message).isEqualTo("Data access error during: Database connection failed")
        assertThat(response.body?.details).isNotNull
        assertThat(response.body?.details?.get("cause")).isEqualTo("Connection refused")
        assertThat(response.body?.details?.get("retryable")).isEqualTo(false)
    }

    @Test
    fun `handleInfrastructureException should add Retry-After header for retryable exceptions`() {
        // given
        val exception = ExternalServiceException("Service unavailable", RuntimeException("Service unavailable"), true)

        // when
        val response = handler.handleInfrastructureException(exception, request)

        // then
        assertThat(response.statusCodeValue).isEqualTo(503)
        assertThat(response.headers.getFirst("Retry-After")).isEqualTo("60")
    }

    @Test
    fun `handleInfrastructureException should map exception types to error codes`() {
        // given
        val dataAccessException = DataAccessException("DB error", RuntimeException("DB connection failed"))
        val externalServiceException = ExternalServiceException("API error", RuntimeException("API timeout"))
        val mailDeliveryException = MailDeliveryException("SMTP error")

        // when
        val dataAccessResponse = handler.handleInfrastructureException(dataAccessException, request)
        val externalServiceResponse = handler.handleInfrastructureException(externalServiceException, request)
        val mailDeliveryResponse = handler.handleInfrastructureException(mailDeliveryException, request)

        // then
        assertThat(dataAccessResponse.body?.errorCode).isEqualTo("DATA_ACCESS_ERROR")
        assertThat(externalServiceResponse.body?.errorCode).isEqualTo("EXTERNAL_SERVICE_ERROR")
        assertThat(mailDeliveryResponse.body?.errorCode).isEqualTo("MAIL_DELIVERY_ERROR")
    }

    @Test
    fun `should handle missing Accept-Language header gracefully`() {
        // given
        val exception = InvalidEmailException("test@invalid")
        `when`(request.getHeader("Accept-Language")).thenReturn(null)

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.statusCodeValue).isEqualTo(400)
        assertThat(response.body?.errorCode).isEqualTo("USER_001")
    }

    @Test
    fun `should handle invalid Accept-Language header gracefully`() {
        // given
        val exception = InvalidEmailException("test@invalid")
        `when`(request.getHeader("Accept-Language")).thenReturn("invalid-locale")

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.statusCodeValue).isEqualTo(400)
        assertThat(response.body?.errorCode).isEqualTo("USER_001")
    }

    @Test
    fun `should handle unsupported locale by falling back to English`() {
        // given
        val exception = InvalidEmailException("test@invalid")
        `when`(request.getHeader("Accept-Language")).thenReturn("fr-FR") // French not supported
        `when`(
            messageSource.getMessage(
                "error.user.001",
                null,
                "User 001",
                Locale.ENGLISH,
            ),
        ).thenReturn("Invalid email format")

        // when
        val response = handler.handleDomainException(exception, request)

        // then
        assertThat(response.body?.localizedMessage).isEqualTo("Invalid email format")
    }
}
