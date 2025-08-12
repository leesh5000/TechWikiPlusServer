package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.common.interfaces.ErrorResponse
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["me.helloc.techwikiplus.service.user"])
class UserServiceExceptionHandler(
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
}
