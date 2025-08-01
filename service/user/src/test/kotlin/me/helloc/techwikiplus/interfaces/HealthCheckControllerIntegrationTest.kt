package me.helloc.techwikiplus.interfaces

import me.helloc.techwikiplus.test.BaseIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
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
 */
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-user",
        "spring.application.version=1.0.0-INTEGRATION",
    ],
)
class HealthCheckControllerIntegrationTest : BaseIntegrationTest() {
    /**
     * 테스트 클래스에서 필드 주입 사용 이유:
     * - Spring 테스트 프레임워크의 제약으로 인해 테스트 클래스는 생성자 주입을 지원하지 않음
     * - MockMvc와 같은 테스트 전용 빈은 Spring의 테스트 인프라에서 특별히 관리됨
     * - 프로덕션 코드와 달리 테스트 코드에서는 필드 주입이 일반적이고 허용되는 패턴
     */
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET health - should return UP status with full application context`() {
        // when & then
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.version").value("1.0.0-INTEGRATION"))
            .andExpect(jsonPath("$.serviceName").value("techwikiplus-user"))
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
