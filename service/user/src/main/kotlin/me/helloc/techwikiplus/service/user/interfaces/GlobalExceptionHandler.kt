package me.helloc.techwikiplus.service.user.interfaces

import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.exception.PasswordMismatchException
import me.helloc.techwikiplus.service.user.domain.exception.PasswordPolicyViolationException
import me.helloc.techwikiplus.service.user.domain.exception.UserAlreadyExistsException
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotActiveException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(e: ValidationException): ResponseEntity<ValidationErrorResponse> {
        logger.warn("Validation error - field: ${e.field}, code: ${e.errorCode}, message: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ValidationErrorResponse(
                    code = "VALIDATION_ERROR",
                    message = "입력값 검증에 실패했습니다",
                    errors =
                        listOf(
                            FieldError(
                                field = e.field,
                                code = e.errorCode,
                                message = e.message ?: "Validation failed",
                            ),
                        ),
                ),
            )
    }

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExists(e: UserAlreadyExistsException): ResponseEntity<ErrorResponse> {
        logger.warn("User already exists: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    code = "USER_ALREADY_EXISTS",
                    message = e.message ?: "User already exists",
                ),
            )
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("User not found: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "USER_NOT_FOUND",
                    message = e.message ?: "User not found",
                ),
            )
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(e: InvalidCredentialsException): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid credentials attempt")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    code = "INVALID_CREDENTIALS",
                    message = e.message ?: "Invalid credentials",
                ),
            )
    }

    @ExceptionHandler(UserNotActiveException::class)
    fun handleUserNotActive(e: UserNotActiveException): ResponseEntity<ErrorResponse> {
        logger.warn("User not active: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ErrorResponse(
                    code = "USER_NOT_ACTIVE",
                    message = e.message ?: "User account is not active",
                ),
            )
    }

    @ExceptionHandler(PasswordMismatchException::class)
    fun handlePasswordMismatch(e: PasswordMismatchException): ResponseEntity<ErrorResponse> {
        logger.warn("Password mismatch: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "PASSWORD_MISMATCH",
                    message = e.message ?: "Password mismatch",
                ),
            )
    }

    @ExceptionHandler(PasswordPolicyViolationException::class)
    fun handlePasswordPolicyViolation(e: PasswordPolicyViolationException): ResponseEntity<ErrorResponse> {
        logger.warn("Password policy violation: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "PASSWORD_POLICY_VIOLATION",
                    message = e.message ?: "Password does not meet requirements",
                ),
            )
    }

    @ExceptionHandler(UserDomainException::class)
    fun handleUserDomainException(e: UserDomainException): ResponseEntity<ErrorResponse> {
        logger.warn("Domain exception: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "DOMAIN_ERROR",
                    message = e.message ?: "Domain error occurred",
                ),
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument: ${e.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "INVALID_ARGUMENT",
                    message = e.message ?: "Invalid argument",
                ),
            )
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(e: RuntimeException): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected runtime exception", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    code = "INTERNAL_ERROR",
                    message = "An unexpected error occurred",
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    code = "INTERNAL_ERROR",
                    message = "An unexpected error occurred",
                ),
            )
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class ValidationErrorResponse(
    val code: String,
    val message: String,
    val errors: List<FieldError>,
    val timestamp: Long = System.currentTimeMillis(),
)

data class FieldError(
    val field: String,
    val code: String,
    val message: String,
)
