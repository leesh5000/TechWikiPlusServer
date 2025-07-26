package me.helloc.techwikiplus.user.interfaces.http

import com.fasterxml.jackson.databind.ObjectMapper
import me.helloc.techwikiplus.user.infrastructure.config.IntegrationTestSupport
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * GlobalExceptionHandler 통합 테스트
 * 실제 Spring 컨텍스트에서 예외 처리가 올바르게 동작하는지 검증
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerIntegrationTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("잘못된 JSON 요청 시 400 에러와 적절한 메시지가 반환된다")
    fun shouldHandleInvalidJsonRequest() {
        // given
        val invalidJson = """{"email": "test@example.com", "password": "pass123}""" // 따옴표 누락

        // when & then
        mockMvc.perform(
            post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/users/login"))
    }

    @Test
    @DisplayName("필수 파라미터 누락 시 400 에러가 반환된다")
    fun shouldHandleMissingParameter() {
        // when & then
        mockMvc.perform(
            get("/api/v1/users/verify")
                // email 파라미터 누락
                .param("code", "123456"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
            .andExpect(jsonPath("$.message").value("Required parameter 'email' is missing"))
    }

    @Test
    @DisplayName("한국어 Accept-Language 헤더 사용 시 localizedMessage가 포함된다")
    fun shouldIncludeLocalizedMessageForKorean() {
        // given
        val invalidEmail =
            mapOf(
                "email" to "invalid-email-format",
                "password" to "Password123!",
            )

        // when & then
        mockMvc.perform(
            post("/api/v1/users/login")
                .header("Accept-Language", "ko-KR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmail)),
        )
            .andExpect(status().isUnauthorized) // 로그인 실패
            .andExpect(jsonPath("$.localizedMessage").exists())
    }

    @Test
    @DisplayName("영어 Accept-Language 헤더 사용 시 영어 메시지가 반환된다")
    fun shouldReturnEnglishMessageForEnglishLocale() {
        // given
        val invalidEmail =
            mapOf(
                "email" to "invalid-email-format",
                "password" to "Password123!",
            )

        // when & then
        mockMvc.perform(
            post("/api/v1/users/login")
                .header("Accept-Language", "en-US")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmail)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.localizedMessage").exists())
    }
}
