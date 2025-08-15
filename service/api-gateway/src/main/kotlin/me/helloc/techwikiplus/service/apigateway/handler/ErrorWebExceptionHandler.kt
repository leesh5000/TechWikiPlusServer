package me.helloc.techwikiplus.service.apigateway.handler

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.ConnectException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeoutException

@Configuration
@Order(-1) // 기본 에러 핸들러보다 우선 실행
class ErrorWebExceptionHandler(
    private val objectMapper: ObjectMapper,
) : ErrorWebExceptionHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(ErrorWebExceptionHandler::class.java)
        private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    }

    override fun handle(
        exchange: ServerWebExchange,
        ex: Throwable,
    ): Mono<Void> {
        val response = exchange.response
        val request = exchange.request
        val requestId = request.headers.getFirst("X-Request-ID") ?: "unknown"

        // 에러 상태와 메시지 결정
        val errorInfo = determineErrorInfo(ex)

        // 로깅
        logError(ex, request.uri.toString(), requestId, errorInfo)

        // 응답 설정
        response.statusCode = errorInfo.status
        response.headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        response.headers.add("X-Request-ID", requestId)

        // 에러 응답 바디 생성
        val errorResponse = createErrorResponse(errorInfo, requestId, request.uri.toString())

        val dataBuffer =
            response.bufferFactory().wrap(
                objectMapper.writeValueAsBytes(errorResponse),
            )

        return response.writeWith(Mono.just(dataBuffer))
    }

    private fun determineErrorInfo(ex: Throwable): ErrorInfo {
        return when (ex) {
            is ResponseStatusException -> {
                ErrorInfo(
                    status = ex.statusCode as HttpStatus,
                    message = ex.reason ?: "Response Status Exception",
                    errorType = "RESPONSE_STATUS_ERROR",
                )
            }
            is ConnectException -> {
                ErrorInfo(
                    status = HttpStatus.BAD_GATEWAY,
                    message = "Service Unavailable - Unable to connect to downstream service",
                    errorType = "SERVICE_UNAVAILABLE",
                )
            }
            is TimeoutException -> {
                ErrorInfo(
                    status = HttpStatus.GATEWAY_TIMEOUT,
                    message = "Gateway Timeout - Request timeout while connecting to service",
                    errorType = "GATEWAY_TIMEOUT",
                )
            }
            is IllegalArgumentException -> {
                ErrorInfo(
                    status = HttpStatus.BAD_REQUEST,
                    message = "Bad Request - Invalid request parameters",
                    errorType = "INVALID_REQUEST",
                )
            }
            else -> {
                ErrorInfo(
                    status = HttpStatus.INTERNAL_SERVER_ERROR,
                    message = "Internal Server Error - An unexpected error occurred",
                    errorType = "INTERNAL_ERROR",
                )
            }
        }
    }

    private fun logError(
        ex: Throwable,
        uri: String,
        requestId: String,
        errorInfo: ErrorInfo,
    ) {
        val timestamp = LocalDateTime.now().format(timeFormatter)

        when (errorInfo.status.value()) {
            in 400..499 -> {
                logger.warn(
                    "[$timestamp] [CLIENT_ERROR] {} - Status: {}, URI: {}, RequestId: {}, Error: {}",
                    errorInfo.errorType,
                    errorInfo.status.value(),
                    uri,
                    requestId,
                    ex.message,
                )
            }
            in 500..599 -> {
                logger.error(
                    "[$timestamp] [SERVER_ERROR] {} - Status: {}, URI: {}, RequestId: {}, Error: {}",
                    errorInfo.errorType,
                    errorInfo.status.value(),
                    uri,
                    requestId,
                    ex.message,
                    ex,
                )
            }
            else -> {
                logger.info(
                    "[$timestamp] [OTHER_ERROR] {} - Status: {}, URI: {}, RequestId: {}, Error: {}",
                    errorInfo.errorType,
                    errorInfo.status.value(),
                    uri,
                    requestId,
                    ex.message,
                )
            }
        }
    }

    private fun createErrorResponse(
        errorInfo: ErrorInfo,
        requestId: String,
        path: String,
    ): ErrorResponse {
        return ErrorResponse(
            timestamp = LocalDateTime.now().format(timeFormatter),
            status = errorInfo.status.value(),
            error = errorInfo.status.reasonPhrase,
            message = errorInfo.message,
            path = path,
            requestId = requestId,
            errorType = errorInfo.errorType,
        )
    }

    data class ErrorInfo(
        val status: HttpStatus,
        val message: String,
        val errorType: String,
    )

    data class ErrorResponse(
        val timestamp: String,
        val status: Int,
        val error: String,
        val message: String,
        val path: String,
        val requestId: String,
        val errorType: String,
    )
}
