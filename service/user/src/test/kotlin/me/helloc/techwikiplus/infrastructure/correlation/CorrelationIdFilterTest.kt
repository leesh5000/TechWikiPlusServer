package me.helloc.techwikiplus.infrastructure.correlation

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.domain.correlation.CorrelationId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("CorrelationIdFilter 테스트")
class CorrelationIdFilterTest {
    private lateinit var filter: CorrelationIdFilter
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        filter = CorrelationIdFilter()
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        filterChain = mock(FilterChain::class.java)
        MDC.clear()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Nested
    @DisplayName("요청에 Correlation ID가 없을 때")
    inner class WhenNoCorrelationIdInRequest {
        @Test
        @DisplayName("새로운 Correlation ID를 생성하고 MDC에 설정한다")
        fun `should generate new correlation id and set in MDC`() {
            // when
            filter.doFilter(request, response, filterChain)

            // then
            verify(filterChain).doFilter(request, response)

            val correlationIdFromMdc = capturedMdcValue()
            assertNotNull(correlationIdFromMdc)

            // UUID 형식 검증
            CorrelationId(correlationIdFromMdc!!) // 유효하지 않으면 예외 발생
        }

        @Test
        @DisplayName("생성된 Correlation ID를 응답 헤더에 추가한다")
        fun `should add generated correlation id to response header`() {
            // when
            filter.doFilter(request, response, filterChain)

            // then
            val headerValue = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)
            assertNotNull(headerValue)

            // UUID 형식 검증
            CorrelationId(headerValue!!) // 유효하지 않으면 예외 발생
        }
    }

    @Nested
    @DisplayName("요청에 Correlation ID가 있을 때")
    inner class WhenCorrelationIdInRequest {
        private val existingCorrelationId = "550e8400-e29b-41d4-a716-446655440000"

        @BeforeEach
        fun setUp() {
            request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingCorrelationId)
        }

        @Test
        @DisplayName("요청 헤더의 Correlation ID를 MDC에 설정한다")
        fun `should use correlation id from request header`() {
            // when
            filter.doFilter(request, response, filterChain)

            // then
            verify(filterChain).doFilter(request, response)

            val correlationIdFromMdc = capturedMdcValue()
            assertEquals(existingCorrelationId, correlationIdFromMdc)
        }

        @Test
        @DisplayName("요청 헤더의 Correlation ID를 응답 헤더에 추가한다")
        fun `should echo correlation id to response header`() {
            // when
            filter.doFilter(request, response, filterChain)

            // then
            val headerValue = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)
            assertEquals(existingCorrelationId, headerValue)
        }
    }

    @Nested
    @DisplayName("MDC 정리")
    inner class MdcCleanup {
        @Test
        @DisplayName("필터 처리 후 MDC를 정리한다")
        fun `should clear MDC after filter processing`() {
            // given
            var mdcValueDuringProcessing: String? = null
            whenever(filterChain.doFilter(any(), any())).then {
                mdcValueDuringProcessing = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
                Unit
            }

            // when
            filter.doFilter(request, response, filterChain)

            // then
            assertNotNull(mdcValueDuringProcessing, "MDC should have value during processing")
            assertEquals(
                null,
                MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY),
                "MDC should be cleared after processing",
            )
        }

        @Test
        @DisplayName("예외가 발생해도 MDC를 정리한다")
        fun `should clear MDC even when exception occurs`() {
            // given
            whenever(filterChain.doFilter(any(), any())).thenThrow(RuntimeException("Test exception"))

            // when & then
            try {
                filter.doFilter(request, response, filterChain)
            } catch (e: RuntimeException) {
                // Expected exception
            }

            assertEquals(
                null,
                MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY),
                "MDC should be cleared after exception",
            )
        }
    }

    @Nested
    @DisplayName("잘못된 Correlation ID 처리")
    inner class InvalidCorrelationId {
        @Test
        @DisplayName("잘못된 형식의 Correlation ID가 있으면 새로운 ID를 생성한다")
        fun `should generate new id when invalid correlation id in request`() {
            // given
            request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "invalid-correlation-id")

            // when
            filter.doFilter(request, response, filterChain)

            // then
            val correlationIdFromMdc = capturedMdcValue()
            assertNotNull(correlationIdFromMdc)
            // 새로 생성된 ID는 유효한 UUID 형식이어야 함
            CorrelationId(correlationIdFromMdc!!)
        }

        @Test
        @DisplayName("빈 Correlation ID가 있으면 새로운 ID를 생성한다")
        fun `should generate new id when empty correlation id in request`() {
            // given
            request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "")

            // when
            filter.doFilter(request, response, filterChain)

            // then
            val correlationIdFromMdc = capturedMdcValue()
            assertNotNull(correlationIdFromMdc)
            // 새로 생성된 ID는 유효한 UUID 형식이어야 함
            CorrelationId(correlationIdFromMdc!!)
        }
    }

    private fun capturedMdcValue(): String? {
        var capturedValue: String? = null
        val httpServletRequest = mock(HttpServletRequest::class.java)
        val httpServletResponse = mock(HttpServletResponse::class.java)
        val mockFilterChain =
            FilterChain { _, _ ->
                capturedValue = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
            }

        filter.doFilter(request, response, mockFilterChain)
        return capturedValue
    }
}
