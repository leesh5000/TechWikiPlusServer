package me.helloc.techwikiplus.user.interfaces.http

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import jakarta.servlet.http.HttpServletRequest
import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.user.infrastructure.exception.DataAccessException
import me.helloc.techwikiplus.user.infrastructure.exception.ExternalServiceException
import me.helloc.techwikiplus.user.infrastructure.exception.InfrastructureException
import me.helloc.techwikiplus.user.infrastructure.exception.MailDeliveryException
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@RestControllerAdvice
class GlobalExceptionHandler(
    private val messageSource: MessageSource? = null,
    private val environment: Environment? = null,
) {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    private val formatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC)

    @ExceptionHandler(InfrastructureException::class)
    fun handleInfrastructureException(
        ex: InfrastructureException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        // 로깅 전략: 인프라 예외는 항상 ERROR 레벨로 로깅
        log.error("Infrastructure error occurred: ${ex.message}", ex)

        val errorCode =
            when (ex) {
                is DataAccessException -> "DATA_ACCESS_ERROR"
                is ExternalServiceException -> "EXTERNAL_SERVICE_ERROR"
                is MailDeliveryException -> "MAIL_DELIVERY_ERROR"
                else -> if (ex.retryable) "INFRA_ERROR_RETRYABLE" else "INFRA_ERROR"
            }

        val status = if (ex.retryable) 503 else 500
        val now = Instant.now()

        // 환경별 메시지 처리
        val (message, details) = getEnvironmentAwareMessage(ex)

        // 국제화된 메시지 가져오기
        val locale = getLocaleFromRequest(request)
        val localizedMessage = getLocalizedMessage(errorCode, locale)

        val errorResponse =
            ErrorResponse(
                errorCode = errorCode,
                message = message,
                timestamp = formatter.format(now),
                path = request.requestURI,
                localizedMessage = localizedMessage,
                details = details,
            )

        return if (ex.retryable) {
            ResponseEntity
                .status(status)
                .header("Retry-After", "60") // 60초 후 재시도
                .body(errorResponse)
        } else {
            ResponseEntity.status(status).body(errorResponse)
        }
    }

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        ex: DomainException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val status =
            when (ex.errorCode) {
                // Validation Errors - 400
                ErrorCode.INVALID_EMAIL,
                ErrorCode.INVALID_NICKNAME,
                ErrorCode.INVALID_PASSWORD,
                ErrorCode.ALREADY_VERIFIED_EMAIL,
                -> 400

                // Not Found Errors - 404
                ErrorCode.USER_NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND,
                -> 404

                // Conflict Errors - 409
                ErrorCode.DUPLICATE_EMAIL,
                ErrorCode.DUPLICATE_NICKNAME,
                -> 409

                // Authentication Errors - 401
                ErrorCode.EXPIRED_EMAIL_VERIFICATION,
                ErrorCode.PENDING_USER_NOT_FOUND,
                ErrorCode.INVALID_VERIFICATION_CODE,
                ErrorCode.INVALID_CREDENTIALS,
                ErrorCode.UNAUTHORIZED_ACCESS,
                ErrorCode.EMAIL_NOT_VERIFIED,
                ErrorCode.INVALID_TOKEN,
                ErrorCode.INVALID_TOKEN_TYPE,
                ErrorCode.ACCOUNT_BANNED,
                ErrorCode.ACCOUNT_DORMANT,
                ErrorCode.ACCOUNT_DELETED,
                -> 401

                // Rate Limit Errors - 429
                ErrorCode.RATE_LIMIT_EXCEEDED -> 429
            }

        // 로깅 전략: 도메인 예외는 WARN 레벨로 로깅 (비즈니스 규칙 위반)
        log.warn("Domain exception occurred: ${ex.message}")

        val now = Instant.now()
        val locale = getLocaleFromRequest(request)
        val localizedMessage = getLocalizedMessage(ex.code, locale)

        val errorResponse =
            ErrorResponse(
                errorCode = ex.code,
                message = ex.message ?: "An error occurred",
                timestamp = formatter.format(now),
                path = request.requestURI,
                localizedMessage = localizedMessage,
            )
        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.warn("Missing parameter: ${ex.parameterName}")

        val now = Instant.now()
        val locale = getLocaleFromRequest(request)
        val localizedMessage = getLocalizedMessage("MISSING_PARAMETER", locale)

        val errorResponse =
            ErrorResponse(
                errorCode = "MISSING_PARAMETER",
                message = "Required parameter '${ex.parameterName}' is missing",
                timestamp = formatter.format(now),
                path = request.requestURI,
                localizedMessage = localizedMessage,
            )
        return ResponseEntity.status(400).body(errorResponse)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val now: Instant = Instant.now()

        val message =
            when (val cause = ex.cause) {
                is JsonParseException -> {
                    val errorMessage = cause.message ?: "Invalid JSON format"
                    when {
                        errorMessage.contains("Unrecognized character escape") -> {
                            val invalidChar = errorMessage.substringAfter("escape '").substringBefore("' (code")
                            "Invalid JSON format: Character '$invalidChar' cannot be escaped. " +
                                "Please check your JSON syntax."
                        }
                        errorMessage.contains("Unexpected character") -> {
                            "Invalid JSON format: Unexpected character found. Please check your JSON syntax."
                        }
                        else -> "Invalid JSON format: $errorMessage"
                    }
                }
                is JsonMappingException -> {
                    val fieldPath = cause.path.joinToString(".") { it.fieldName ?: "[${it.index}]" }
                    "Invalid JSON format in field '$fieldPath': ${cause.originalMessage}"
                }
                else -> "Invalid request body: Unable to parse JSON. Please check your request format."
            }

        log.debug("JSON parsing error: ${ex.message}", ex)

        val locale = getLocaleFromRequest(request)
        val localizedMessage = getLocalizedMessage("INVALID_JSON", locale)

        val errorResponse =
            ErrorResponse(
                errorCode = "INVALID_JSON",
                message = message,
                timestamp = formatter.format(now),
                path = request.requestURI,
                localizedMessage = localizedMessage,
            )
        return ResponseEntity.status(400).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error occurred", ex)

        val now = Instant.now()
        val (message, details) = getEnvironmentAwareMessage(ex)
        val locale = getLocaleFromRequest(request)
        val localizedMessage = getLocalizedMessage("INTERNAL_SERVER_ERROR", locale)

        val errorResponse =
            ErrorResponse(
                errorCode = "INTERNAL_SERVER_ERROR",
                message = message,
                timestamp = formatter.format(now),
                path = request.requestURI,
                localizedMessage = localizedMessage,
                details = details,
            )
        return ResponseEntity.status(500).body(errorResponse)
    }

    // 환경별 메시지 처리
    private fun getEnvironmentAwareMessage(ex: Exception): Pair<String, Map<String, Any>?> {
        val activeProfiles = environment?.activeProfiles ?: emptyArray()
        val isProduction = activeProfiles.contains("production") || activeProfiles.contains("prod")

        return if (isProduction) {
            // Production: 민감한 정보 숨기기
            val genericMessage =
                when (ex) {
                    is InfrastructureException -> "An error occurred while processing your request"
                    else -> "An unexpected error occurred"
                }
            Pair(genericMessage, null)
        } else {
            // Development/Test: 상세 정보 포함
            val details = mutableMapOf<String, Any>()
            ex.cause?.let { details["cause"] = it.message ?: it.javaClass.simpleName }
            if (ex is InfrastructureException) {
                details["retryable"] = ex.retryable
            }
            Pair(ex.message ?: "Unknown error", details)
        }
    }

    // Accept-Language 헤더에서 Locale 추출
    private fun getLocaleFromRequest(request: HttpServletRequest): Locale {
        val acceptLanguage = request.getHeader("Accept-Language")
        return if (!acceptLanguage.isNullOrBlank()) {
            try {
                Locale.forLanguageTag(acceptLanguage.split(",")[0].trim())
            } catch (e: Exception) {
                Locale.ENGLISH
            }
        } else {
            Locale.ENGLISH
        }
    }

    // 국제화된 메시지 가져오기
    private fun getLocalizedMessage(
        errorCode: String,
        locale: Locale,
    ): String {
        if (messageSource == null) {
            return getDefaultMessage(errorCode)
        }

        val messageKey = "error.${errorCode.lowercase().replace('_', '.')}"
        return try {
            // 한국어와 영어만 지원, 그 외는 영어로 fallback
            val supportedLocale =
                when (locale.language) {
                    "ko" -> Locale.KOREA
                    else -> Locale.ENGLISH
                }
            messageSource.getMessage(messageKey, null, getDefaultMessage(errorCode), supportedLocale)
                ?: getDefaultMessage(errorCode)
        } catch (e: Exception) {
            log.debug("Failed to get localized message for key: $messageKey", e)
            getDefaultMessage(errorCode)
        }
    }

    // 기본 메시지 제공
    private fun getDefaultMessage(errorCode: String): String {
        return when (errorCode) {
            "DATA_ACCESS_ERROR" -> "Data access error occurred"
            "EXTERNAL_SERVICE_ERROR" -> "External service error occurred"
            "MAIL_DELIVERY_ERROR" -> "Mail delivery error occurred"
            "INFRA_ERROR_RETRYABLE" -> "Service temporarily unavailable, please try again"
            "INFRA_ERROR" -> "Infrastructure error occurred"
            "MISSING_PARAMETER" -> "Required parameter is missing"
            "INVALID_JSON" -> "Invalid JSON format"
            "INTERNAL_SERVER_ERROR" -> "An unexpected error occurred"
            else ->
                errorCode.replace('_', ' ').lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }
}
