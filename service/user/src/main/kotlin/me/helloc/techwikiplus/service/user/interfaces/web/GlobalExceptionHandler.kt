package me.helloc.techwikiplus.service.user.interfaces.web

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["me.helloc.techwikiplus.service.user"])
class GlobalExceptionHandler(
    private val userErrorCodeMapper: UserErrorCodeMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(UserDomainException::class)
    fun handleDomainException(e: UserDomainException): ResponseEntity<ErrorResponse> {
        val httpStatus = userErrorCodeMapper.mapToHttpStatus(e.userErrorCode)
        val message = userErrorCodeMapper.mapToMessage(e.userErrorCode, e.params)

        logger.warn("Domain exception occurred - ErrorCode: {}, Status: {}", e.userErrorCode, httpStatus)

        return ResponseEntity
            .status(httpStatus)
            .body(ErrorResponse.Companion.of(e.userErrorCode.name, message))
    }


    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.warn("Failed to read HTTP message")

        val cause = e.cause
        if (cause is MismatchedInputException) {
            val fieldName = cause.path.firstOrNull()?.fieldName
            val safeFieldName = fieldName ?: "unknown"
            val safeMessage = "필수 필드 '$safeFieldName'이(가) 누락되었습니다"

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.Companion.of("MISSING_REQUIRED_FIELD", safeMessage))
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.Companion.of("INVALID_REQUEST_BODY", "잘못된 요청 형식입니다. JSON 형식을 확인해주세요"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.warn("Validation failed for request")

        val fieldErrors = e.bindingResult.fieldErrors
        val message =
            if (fieldErrors.size == 1) {
                val error = fieldErrors.first()
                "'${error.field}' 필드 검증 실패: ${error.defaultMessage ?: "올바른 값을 입력해주세요"}"
            } else {
                val errorMessages =
                    fieldErrors.joinToString(", ") { error ->
                        "'${error.field}' (${error.defaultMessage ?: "검증 실패"})"
                    }
                "여러 필드 검증 실패: $errorMessages"
            }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.Companion.of("VALIDATION_ERROR", message))
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupported(e: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponse> {
        logger.warn("Unsupported media type: {}", e.contentType)

        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(ErrorResponse.Companion.of("UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다. 요청 타입을 확인하세요"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument detected")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.Companion.of("INVALID_ARGUMENT", e.message ?: "잘못된 인자입니다"))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(e: RuntimeException): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected runtime exception: {}", e.javaClass.simpleName, e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.Companion.of("INTERNAL_ERROR", "예기치 않은 오류가 발생했습니다"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception: {}", e.javaClass.simpleName, e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.Companion.of("INTERNAL_ERROR", "시스템 오류가 발생했습니다"))
    }
}
