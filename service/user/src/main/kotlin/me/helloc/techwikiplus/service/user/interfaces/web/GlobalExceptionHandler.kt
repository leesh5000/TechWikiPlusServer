package me.helloc.techwikiplus.service.user.interfaces.web

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.interfaces.web.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(
    private val errorCodeMapper: ErrorCodeMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        e: DomainException,
    ): ResponseEntity<ErrorResponse> {
        val httpStatus = errorCodeMapper.mapToHttpStatus(e.errorCode)
        val message = errorCodeMapper.mapToMessage(e.errorCode, e.params)
        
        logger.warn("Domain exception occurred - ErrorCode: {}, Status: {}", e.errorCode, httpStatus)
        
        return ResponseEntity
            .status(httpStatus)
            .body(ErrorResponse.of(e.errorCode.name, message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        e: HttpMessageNotReadableException,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Failed to read HTTP message")

        val cause = e.cause
        if (cause is MismatchedInputException) {
            val fieldName = cause.path.firstOrNull()?.fieldName
            val safeFieldName = fieldName ?: "unknown"
            val safeMessage = "필수 필드가 누락되었습니다: $safeFieldName"

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("MISSING_REQUIRED_FIELD", safeMessage))
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("INVALID_REQUEST_BODY", "잘못된 요청 형식입니다"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        e: IllegalArgumentException,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument detected")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("INVALID_ARGUMENT", e.message ?: "잘못된 인자입니다"))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        e: RuntimeException,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected runtime exception: {}", e.javaClass.simpleName, e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of("INTERNAL_ERROR", "예기치 않은 오류가 발생했습니다"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        e: Exception,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception: {}", e.javaClass.simpleName, e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of("INTERNAL_ERROR", "시스템 오류가 발생했습니다"))
    }
}