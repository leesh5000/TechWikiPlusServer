package me.helloc.techwikiplus.user.interfaces.http

import jakarta.servlet.http.HttpServletRequest
import me.helloc.techwikiplus.user.infrastructure.exception.DataAccessException
import me.helloc.techwikiplus.user.infrastructure.exception.ExternalServiceException
import me.helloc.techwikiplus.user.infrastructure.exception.InfrastructureException
import me.helloc.techwikiplus.user.infrastructure.exception.MailDeliveryException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.context.MessageSource
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import java.util.Locale

class GlobalExceptionHandlerAdvancedTest {
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
        `when`(request.getHeader("Accept-Language")).thenReturn("ko-KR")
    }

    @Nested
    @DisplayName("InfrastructureException 처리")
    inner class InfrastructureExceptionHandling {
        @Test
        @DisplayName("일반 InfrastructureException이 500 상태 코드로 처리되어야 한다")
        fun shouldHandleInfrastructureExceptionWith500Status() {
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
        @DisplayName("재시도 가능한 InfrastructureException은 503 상태 코드와 Retry-After 헤더를 포함해야 한다")
        fun shouldHandleRetryableInfrastructureExceptionWith503Status() {
            // given
            val exception = InfrastructureException("Service temporarily unavailable", retryable = true)
            `when`(environment.activeProfiles).thenReturn(arrayOf("development"))

            // when
            val response = handler.handleInfrastructureException(exception, request)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            assertThat(response.headers.getFirst("Retry-After")).isEqualTo("60")
            assertThat(response.body!!.errorCode).isEqualTo("INFRA_ERROR_RETRYABLE")
        }

        @Test
        @DisplayName("DataAccessException이 적절히 처리되어야 한다")
        fun shouldHandleDataAccessException() {
            // given
            val exception = DataAccessException("Failed to save user", cause = RuntimeException("Connection timeout"))
            `when`(environment.activeProfiles).thenReturn(arrayOf("development"))

            // when
            val response = handler.handleInfrastructureException(exception, request)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(response.body!!.errorCode).isEqualTo("DATA_ACCESS_ERROR")
            assertThat(response.body!!.message).contains("Failed to save user")
        }

        @Test
        @DisplayName("ExternalServiceException이 적절히 처리되어야 한다")
        fun shouldHandleExternalServiceException() {
            // given
            val exception =
                ExternalServiceException("Payment gateway error", RuntimeException("Service timeout"), retryable = true)
            `when`(environment.activeProfiles).thenReturn(arrayOf("development"))

            // when
            val response = handler.handleInfrastructureException(exception, request)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            assertThat(response.body!!.errorCode).isEqualTo("EXTERNAL_SERVICE_ERROR")
            assertThat(response.headers.getFirst("Retry-After")).isNotNull
        }

        @Test
        @DisplayName("MailDeliveryException이 적절히 처리되어야 한다")
        fun shouldHandleMailDeliveryException() {
            // given
            val exception = MailDeliveryException("Failed to send email to user@example.com")
            `when`(environment.activeProfiles).thenReturn(arrayOf("development"))

            // when
            val response = handler.handleInfrastructureException(exception, request)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(response.body!!.errorCode).isEqualTo("MAIL_DELIVERY_ERROR")
        }
    }

    @Nested
    @DisplayName("환경별 응답 상세도 조정")
    inner class EnvironmentBasedResponseDetail {
        @Test
        @DisplayName("Production 환경에서는 민감한 정보가 숨겨져야 한다")
        fun shouldHideSensitiveInfoInProduction() {
            // given
            val cause = RuntimeException("Password: secret123 in connection string")
            val exception = DataAccessException("Database error", cause)
            `when`(environment.activeProfiles).thenReturn(arrayOf("production"))

            // when
            val response = handler.handleInfrastructureException(exception, request)

            // then
            assertThat(response.body!!.message).isEqualTo("An error occurred while processing your request")
            assertThat(response.body!!.message).doesNotContain("secret123")
            assertThat(response.body!!.details).isNull()
        }

        @Test
        @DisplayName("Development 환경에서는 상세 정보가 포함되어야 한다")
        fun shouldIncludeDetailedInfoInDevelopment() {
            // given
            val cause = RuntimeException("Connection timeout")
            val exception = DataAccessException("Database error", cause)
            `when`(environment.activeProfiles).thenReturn(arrayOf("development"))

            // when
            val response = handler.handleInfrastructureException(exception, request)

            // then
            assertThat(response.body!!.message).contains("Database error")
            assertThat(response.body!!.details).isNotNull
            assertThat(response.body!!.details).containsKey("cause")
            assertThat(response.body!!.details!!["cause"]).isEqualTo("Connection timeout")
        }

        @Test
        @DisplayName("Test 환경에서는 상세 정보가 포함되어야 한다")
        fun shouldIncludeDetailedInfoInTest() {
            // given
            val exception = InfrastructureException("Test error")
            `when`(environment.activeProfiles).thenReturn(arrayOf("test"))

            // when
            val response = handler.handleInfrastructureException(exception, request)

            // then
            assertThat(response.body!!.message).contains("Test error")
            assertThat(response.body!!.details).isNotNull
        }
    }

    @Nested
    @DisplayName("국제화 지원")
    inner class InternationalizationSupport {
        @Test
        @DisplayName("Accept-Language 헤더에 따라 적절한 언어로 메시지가 반환되어야 한다")
        fun shouldReturnLocalizedErrorMessage() {
            // given
            val exception = DataAccessException("Database error", RuntimeException("Connection failed"))
            `when`(environment.activeProfiles).thenReturn(arrayOf("development"))
            `when`(request.getHeader("Accept-Language")).thenReturn("ko-KR")
            `when`(
                messageSource.getMessage(
                    "error.data_access",
                    null,
                    "데이터 접근 중 오류가 발생했습니다",
                    Locale.KOREA,
                ),
            ).thenReturn("데이터 접근 중 오류가 발생했습니다")

            // when
            val response = handler.handleInfrastructureException(exception, request)

            // then
            assertThat(response.body!!.localizedMessage).isEqualTo("데이터 접근 중 오류가 발생했습니다")
        }

        @Test
        @DisplayName("지원하지 않는 언어의 경우 기본 언어로 메시지가 반환되어야 한다")
        fun shouldFallbackToDefaultLanguage() {
            // given
            val exception = DataAccessException("Database error", RuntimeException("Connection failed"))
            `when`(environment.activeProfiles).thenReturn(arrayOf("development"))
            `when`(request.getHeader("Accept-Language")).thenReturn("fr-FR")
            `when`(
                messageSource.getMessage(
                    "error.data_access",
                    null,
                    "Data access error occurred",
                    Locale.ENGLISH,
                ),
            ).thenReturn("Data access error occurred")

            // when
            val response = handler.handleInfrastructureException(exception, request)

            // then
            assertThat(response.body!!.localizedMessage).isEqualTo("Data access error occurred")
        }
    }
}
