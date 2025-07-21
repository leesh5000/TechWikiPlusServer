package me.helloc.techwikiplus.user.interfaces.http

import jakarta.servlet.http.HttpServletRequest
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@RestControllerAdvice
class GlobalExceptionHandler {

    private val formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .withZone(ZoneOffset.UTC)

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(
        ex: CustomException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val status = when (ex) {
            is CustomException.ValidationException -> 400 // Bad Request
            is CustomException.AuthenticationException -> 401 // Unauthorized
            is CustomException.NotFoundException -> 404 // Not Found
            is CustomException.ConflictException -> 409 // Conflict
            is CustomException.ResendRateLimitExceeded -> 429 // Too Many Requests
        }

        val now: Instant = Instant.now()
        val errorResponse = ErrorResponse(
            errorCode = ex.errorCode,
            message = ex.message,
            timestamp = formatter.format(now),
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val now: Instant = Instant.now()
        val errorResponse = ErrorResponse(
            errorCode = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred.",
            timestamp = formatter.format(now),
            path = request.requestURI
        )
        return ResponseEntity.status(500).body(errorResponse)
    }
}

