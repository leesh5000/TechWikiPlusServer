package me.helloc.techwikiplus.service.common.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import me.helloc.techwikiplus.service.common.interfaces.HealthCheckController
import me.helloc.techwikiplus.service.config.BaseE2eTest
import me.helloc.techwikiplus.service.config.annotations.E2eTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * HealthCheckController 통합 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 운영 환경과 동일한 설정
 * - End-to-End 검증
 * - API 문서 자동 생성 (generateDocs = true)
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-user",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
class HealthCheckControllerE2eTest : BaseE2eTest() {
    @Test
    fun `GET health - 전체 애플리케이션 컨텍스트로 UP 상태를 반환해야 한다`() {
        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/health")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.version").value("1.0.0-INTEGRATION"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.serviceName").value("techwikiplus-user"))
            .andDo(
                documentWithResource(
                    "서비스 상태 확인",
                    ResourceSnippetParameters.Companion.builder()
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
                            PayloadDocumentation.fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("서비스 상태 (UP/DOWN)"),
                            PayloadDocumentation.fieldWithPath("version")
                                .type(JsonFieldType.STRING)
                                .description("서비스 버전"),
                            PayloadDocumentation.fieldWithPath("serviceName")
                                .type(JsonFieldType.STRING)
                                .description("서비스 이름"),
                        )
                        .responseSchema(
                            Schema.Companion.schema(
                                "${HealthCheckController::class.simpleName}" +
                                    ".${HealthCheckController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET health - 데이터베이스 연결과 함께 작동해야 한다`() {
        // given - DB connection is established via TestContainers

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.get("/health"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))

        // This test verifies that the application can start with a real database
        // and the health check endpoint works correctly
    }
}
