package me.helloc.techwikiplus.infrastructure.correlation

import me.helloc.techwikiplus.test.BaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@DisplayName("CorrelationIdFilter 통합 테스트")
class CorrelationIdFilterIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("요청에 Correlation ID가 없으면 새로 생성하여 응답 헤더에 추가한다")
    fun `should generate correlation id when not present`() {
        mockMvc
            .perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect { result ->
                val correlationId = result.response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)
                assertNotNull(correlationId)
                // UUID 형식 검증
                UUID.fromString(correlationId)
            }
    }

    @Test
    @DisplayName("요청에 Correlation ID가 있으면 동일한 ID를 응답 헤더에 반환한다")
    fun `should echo correlation id from request`() {
        val correlationId = UUID.randomUUID().toString()

        mockMvc
            .perform(
                get("/health")
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId),
            )
            .andExpect(status().isOk)
            .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId))
    }

    @Test
    @DisplayName("잘못된 Correlation ID가 있으면 새로운 ID를 생성한다")
    fun `should generate new id when invalid correlation id in request`() {
        val invalidCorrelationId = "not-a-valid-uuid"

        mockMvc
            .perform(
                get("/health")
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, invalidCorrelationId),
            )
            .andExpect(status().isOk)
            .andExpect { result ->
                val correlationId = result.response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)
                assertNotNull(correlationId)
                // 새로 생성된 ID는 유효한 UUID여야 함
                UUID.fromString(correlationId)
                // 원래 잘못된 ID와 달라야 함
                assertEquals(false, correlationId == invalidCorrelationId)
            }
    }
}
