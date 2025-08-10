package me.helloc.techwikiplus.infrastructure.correlation

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.domain.correlation.CorrelationId
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Correlation ID를 처리하는 서블릿 필터
 *
 * 모든 HTTP 요청에 대해 Correlation ID를 설정하고 MDC에 추가합니다.
 * 요청 헤더에 Correlation ID가 있으면 사용하고, 없으면 새로 생성합니다.
 */
@Component
@Order(1)
class CorrelationIdFilter : Filter {
    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val CORRELATION_ID_MDC_KEY = "correlationId"
        private val logger = KotlinLogging.logger {}
    }

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val correlationId = extractOrGenerateCorrelationId(httpRequest)

        try {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId.value)
            httpResponse.addHeader(CORRELATION_ID_HEADER, correlationId.value)

            logger.debug { "Processing request with correlation ID: ${correlationId.value}" }

            chain.doFilter(request, response)
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY)
        }
    }

    private fun extractOrGenerateCorrelationId(request: HttpServletRequest): CorrelationId {
        val headerValue = request.getHeader(CORRELATION_ID_HEADER)

        return if (!headerValue.isNullOrBlank()) {
            try {
                CorrelationId(headerValue)
            } catch (e: IllegalArgumentException) {
                logger.warn { "Invalid correlation ID received: $headerValue. Generating new one." }
                CorrelationId.generate()
            }
        } else {
            CorrelationId.generate()
        }
    }
}
