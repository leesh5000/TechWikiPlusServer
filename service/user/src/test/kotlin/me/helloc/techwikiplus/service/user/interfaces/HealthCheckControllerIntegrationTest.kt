package me.helloc.techwikiplus.service.user.interfaces

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.service.user.config.BaseIntegrationTest
import me.helloc.techwikiplus.service.user.config.annotations.IntegrationTest
import me.helloc.techwikiplus.service.user.interfaces.dto.HealthCheckResponse
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * HealthCheckController 통합 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 운영 환경과 동일한 설정
 * - End-to-End 검증
 * - API 문서 자동 생성 (generateDocs = true)
 */
@IntegrationTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-user",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
class HealthCheckControllerIntegrationTest : BaseIntegrationTest() {
    @Test
    fun `GET health - should return UP status with full application context`() {
        // when & then
        mockMvc.perform(
            get("/health")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.version").value("1.0.0-INTEGRATION"))
            .andExpect(jsonPath("$.serviceName").value("techwikiplus-user"))
            .andDo(
                documentWithResource(
                    "health-check",
                    ResourceSnippetParameters.builder()
                        .tag("Health Check")
                        .summary("서비스 상태 확인")
                        .description(
                            """
                            서비스의 현재 상태를 확인합니다.

                            이 엔드포인트는 로드 밸런서나 모니터링 시스템에서
                            서비스의 가용성을 확인하는 데 사용됩니다.
                            """.trimIndent(),
                        )
                        .responseFields(
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("서비스 상태 (UP/DOWN)"),
                            fieldWithPath("version")
                                .type(JsonFieldType.STRING)
                                .description("서비스 버전"),
                            fieldWithPath("serviceName")
                                .type(JsonFieldType.STRING)
                                .description("서비스 이름"),
                        )
                        .responseSchema(
                            schema(HealthCheckResponse::class.java.simpleName),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET health - should work with database connection`() {
        // given - DB connection is established via TestContainers

        // when & then
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))

        // This test verifies that the application can start with a real database
        // and the health check endpoint works correctly
    }
}
