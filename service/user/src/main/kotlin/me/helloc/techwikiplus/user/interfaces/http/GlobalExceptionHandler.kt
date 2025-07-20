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

        val responseEntity = when (ex) {
            is CustomException.ValidationException -> ResponseEntity.status(400) // Bad Request
            is CustomException.NotFoundException -> ResponseEntity.status(404) // Not Found
            is CustomException.ConflictException -> ResponseEntity.status(409) // Conflict
            // Internal Server Error for any other CustomException
        }

        val now: Instant = Instant.now()
        val errorResponse = ErrorResponse(
            errorCode = ex.errorCode,
            message = ex.message,
            timestamp = formatter.format(now),
            path = request.requestURI
        )
        return responseEntity.body(errorResponse)
    }
}

