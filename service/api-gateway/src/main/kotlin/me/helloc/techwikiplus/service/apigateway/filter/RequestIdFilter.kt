package me.helloc.techwikiplus.service.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class RequestIdFilter : GlobalFilter, Ordered {
    companion object {
        private val logger = LoggerFactory.getLogger(RequestIdFilter::class.java)
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val GATEWAY_REQUEST_ID_HEADER = "X-Gateway-Request-ID"
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val request = exchange.request

        // 클라이언트로부터 전달된 Request ID 확인
        var requestId = request.headers.getFirst(REQUEST_ID_HEADER)

        // Correlation ID 확인 (마이크로서비스 간 요청 추적용)
        var correlationId = request.headers.getFirst(CORRELATION_ID_HEADER)

        // Request ID가 없으면 새로 생성
        if (requestId.isNullOrBlank()) {
            requestId = generateRequestId()
            logger.debug("Generated new Request ID: {}", requestId)
        }

        // Correlation ID가 없으면 Request ID를 사용
        if (correlationId.isNullOrBlank()) {
            correlationId = requestId
            logger.debug("Using Request ID as Correlation ID: {}", correlationId)
        }

        // Gateway에서 생성하는 고유 ID (내부 추적용) - 기존에 있으면 재사용
        val existingGatewayRequestId = request.headers.getFirst(GATEWAY_REQUEST_ID_HEADER)
        val gatewayRequestId = existingGatewayRequestId ?: generateRequestId()

        // 헤더에 추가하여 다운스트림 서비스로 전달
        val mutatedRequest =
            request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(GATEWAY_REQUEST_ID_HEADER, gatewayRequestId)
                .build()

        // 응답 헤더는 필터 체인의 마지막에서만 추가 (중복 방지)

        logger.debug(
            "Request ID tracking - RequestId: {}, CorrelationId: {}, GatewayRequestId: {}, Path: {}",
            requestId,
            correlationId,
            gatewayRequestId,
            request.uri.path,
        )

        // 응답 헤더 미리 설정 (ReadOnly 상태가 되기 전에)
        val response = exchange.response
        response.headers.add(REQUEST_ID_HEADER, requestId)
        response.headers.add(CORRELATION_ID_HEADER, correlationId)
        response.headers.add(GATEWAY_REQUEST_ID_HEADER, gatewayRequestId)

        // 수정된 요청으로 필터 체인 계속 실행
        val mutatedExchange = exchange.mutate().request(mutatedRequest).build()
        return chain.filter(mutatedExchange)
    }

    private fun generateRequestId(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    override fun getOrder(): Int {
        // LoggingGlobalFilter보다 먼저 실행되어야 함 (Request ID가 로그에 포함되도록)
        return Ordered.HIGHEST_PRECEDENCE + 1
    }
}
