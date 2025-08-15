package me.helloc.techwikiplus.service.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class LoggingGlobalFilter : GlobalFilter, Ordered {
    companion object {
        private val logger = LoggerFactory.getLogger(LoggingGlobalFilter::class.java)
        private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val startTime = System.currentTimeMillis()
        val timestamp = LocalDateTime.now().format(timeFormatter)
        val request = exchange.request
        val response = exchange.response

        logRequest(request, timestamp)

        return chain.filter(exchange).then(
            Mono.fromRunnable {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                logResponse(request, response, duration, timestamp)
            },
        )
    }

    private fun logRequest(
        request: ServerHttpRequest,
        timestamp: String,
    ) {
        val method = request.method.toString()
        val uri = request.uri
        val headers = request.headers
        val remoteAddress = request.remoteAddress?.address?.hostAddress ?: "unknown"
        val userAgent = headers.getFirst("User-Agent") ?: "unknown"
        val requestId = headers.getFirst("X-Gateway-Request-Id") ?: "unknown"

        logger.info(
            "[$timestamp] [REQUEST] [{}] {} {} - Remote: {}, User-Agent: {}, RequestId: {}",
            method,
            uri.path,
            uri.query?.let { "?$it" } ?: "",
            remoteAddress,
            userAgent,
            requestId,
        )

        // 디버그 레벨에서 더 자세한 헤더 정보 로그
        if (logger.isDebugEnabled) {
            logger.debug("Request Headers: {}", headers)
        }
    }

    private fun logResponse(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        duration: Long,
        timestamp: String,
    ) {
        val method = request.method.toString()
        val uri = request.uri
        val statusCode = response.statusCode?.value() ?: 0
        val requestId = request.headers.getFirst("X-Gateway-Request-Id") ?: "unknown"

        logger.info(
            "[$timestamp] [RESPONSE] [{}] {} {} - Status: {}, Duration: {}ms, RequestId: {}",
            method,
            uri.path,
            uri.query?.let { "?$it" } ?: "",
            statusCode,
            duration,
            requestId,
        )

        // 에러 응답의 경우 추가 로깅
        if (statusCode >= 400) {
            logger.warn(
                "Error Response - Status: {}, Path: {}, Duration: {}ms, RequestId: {}",
                statusCode,
                uri.path,
                duration,
                requestId,
            )
        }

        // 디버그 레벨에서 응답 헤더 정보 로그
        if (logger.isDebugEnabled) {
            logger.debug("Response Headers: {}", response.headers)
        }
    }

    override fun getOrder(): Int {
        // 가장 높은 우선순위로 실행 (낮은 숫자일수록 높은 우선순위)
        return Ordered.HIGHEST_PRECEDENCE
    }
}
