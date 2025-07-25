package me.helloc.techwikiplus.user.interfaces.http

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import jakarta.servlet.http.HttpServletRequest
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    private val formatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC)

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(
        ex: CustomException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val status =
            when (ex) {
                is CustomException.ValidationException -> 400 // Bad Request
                is CustomException.AuthenticationException -> 401 // Unauthorized
                is CustomException.NotFoundException -> 404 // Not Found
                is CustomException.ConflictException -> 409 // Conflict
                is CustomException.ResendRateLimitExceeded -> 429 // Too Many Requests
            }

        val now: Instant = Instant.now()
        val errorResponse =
            ErrorResponse(
                errorCode = ex.errorCode,
                message = ex.message,
                timestamp = formatter.format(now),
                path = request.requestURI,
            )
        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val now: Instant = Instant.now()
        val errorResponse =
            ErrorResponse(
                errorCode = "MISSING_PARAMETER",
                message = "Required parameter '${ex.parameterName}' is missing",
                timestamp = formatter.format(now),
                path = request.requestURI,
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
                            "Invalid JSON format: Character '$invalidChar' cannot be escaped. Please check your JSON syntax."
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

        val errorResponse =
            ErrorResponse(
                errorCode = "INVALID_JSON",
                message = message,
                timestamp = formatter.format(now),
                path = request.requestURI,
            )
        return ResponseEntity.status(400).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error occurred: ${ex.message}", ex)
        val now: Instant = Instant.now()
        val errorResponse =
            ErrorResponse(
                errorCode = "INTERNAL_SERVER_ERROR",
                message = "An unexpected error occurred.",
                timestamp = formatter.format(now),
                path = request.requestURI,
            )
        return ResponseEntity.status(500).body(errorResponse)
    }
}
