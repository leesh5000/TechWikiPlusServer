package me.helloc.techwikiplus.documentation

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.interfaces.dto.HealthCheckResponse
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Health Check API 문서화 테스트
 *
 * 이 테스트는 Health Check API의 동작을 검증하고
 * 동시에 API 문서를 자동으로 생성합니다.
 */
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-user",
        "spring.application.version=TEST_VERSION",
    ],
)
class HealthCheckApiDocumentationTest : ApiDocumentationTest() {
    @Test
    fun `GET health - retrieve service status`() {
        // given
        val expectedStatus = "UP"
        val expectedVersion = "TEST_VERSION"
        val expectedServiceName = "techwikiplus-user"

        // when and then
        mockMvc.perform(
            get("/health")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(expectedStatus))
            .andExpect(jsonPath("$.version").value(expectedVersion))
            .andExpect(jsonPath("$.serviceName").value(expectedServiceName))
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
}
