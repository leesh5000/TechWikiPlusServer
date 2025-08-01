package me.helloc.techwikiplus.service.user.interfaces

import me.helloc.techwikiplus.test.BaseUnitTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * HealthCheckController 단위 테스트
 *
 * - 컨트롤러 레이어만 테스트
 * - 외부 의존성 없음
 * - 빠른 실행 (밀리초 단위)
 * - FIRST 원칙 준수
 */
@WebMvcTest(HealthCheckController::class)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-user",
        "spring.application.version=1.0.0-TEST",
    ],
)
class HealthCheckControllerUnitTest : BaseUnitTest() {
    /**
     * 테스트 클래스에서 필드 주입 사용 이유:
     * - Spring 테스트 프레임워크의 제약으로 인해 테스트 클래스는 생성자 주입을 지원하지 않음
     * - MockMvc와 같은 테스트 전용 빈은 Spring의 테스트 인프라에서 특별히 관리됨
     * - 프로덕션 코드와 달리 테스트 코드에서는 필드 주입이 일반적이고 허용되는 패턴
     */
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET health - should return UP status with service information`() {
        // when & then
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.version").value("1.0.0-TEST"))
            .andExpect(jsonPath("$.serviceName").value("techwikiplus-user"))
    }

    @Test
    fun `GET health - should return consistent response on multiple calls`() {
        // given
        val expectedStatus = "UP"
        val expectedVersion = "1.0.0-TEST"
        val expectedServiceName = "techwikiplus-user"

        // when & then - First call
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(expectedStatus))
            .andExpect(jsonPath("$.version").value(expectedVersion))
            .andExpect(jsonPath("$.serviceName").value(expectedServiceName))

        // when & then - Second call (repeatability test)
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(expectedStatus))
            .andExpect(jsonPath("$.version").value(expectedVersion))
            .andExpect(jsonPath("$.serviceName").value(expectedServiceName))
    }
}
