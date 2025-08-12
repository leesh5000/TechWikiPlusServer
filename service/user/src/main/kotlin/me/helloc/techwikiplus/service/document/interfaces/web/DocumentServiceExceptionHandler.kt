package me.helloc.techwikiplus.service.document.interfaces.web

import me.helloc.techwikiplus.service.common.interfaces.ErrorResponse
import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["me.helloc.techwikiplus.service.document"])
class DocumentServiceExceptionHandler(
    private val documentErrorCodeMapper: DocumentErrorCodeMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(DocumentDomainException::class)
    fun handleDomainException(e: DocumentDomainException): ResponseEntity<ErrorResponse> {
        val httpStatus = documentErrorCodeMapper.mapToHttpStatus(e.documentErrorCode)
        val message = documentErrorCodeMapper.mapToMessage(e.documentErrorCode, e.params)

        logger.warn("Domain exception occurred - ErrorCode: {}, Status: {}", e.documentErrorCode, httpStatus)

        return ResponseEntity
            .status(httpStatus)
            .body(ErrorResponse.Companion.of(e.documentErrorCode.name, message))
    }
}
