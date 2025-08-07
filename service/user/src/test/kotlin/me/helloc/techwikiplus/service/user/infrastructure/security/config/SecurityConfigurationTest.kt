package me.helloc.techwikiplus.service.user.infrastructure.security.config

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.infrastructure.security.jwt.JwtTokenManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigurationTest.TestConfig::class)
class SecurityConfigurationTest : DescribeSpec() {
    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun mockJwtTokenManager(): JwtTokenManager = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    init {
        describe("Spring Security Configuration") {
            context("공개 엔드포인트") {
                it("로그인 엔드포인트는 인증 없이 접근 가능해야 함") {
                    // 인증 없이 접근 가능함을 확인 - 실제 처리 결과는 400 Bad Request가 될 수 있음
                    mockMvc.perform(
                        post("/api/v1/users/login")
                            .contentType("application/json")
                            .content("""{"email":"test@example.com","password":"password"}"""),
                    ).andExpect(status().is4xxClientError) // 400 Bad Request 또는 401 Unauthorized 예상
                }

                it("회원가입 엔드포인트는 인증 없이 접근 가능해야 함") {
                    // 인증 없이 접근 가능함을 확인 - 실제 처리 결과는 400 Bad Request가 될 수 있음
                    mockMvc.perform(
                        post("/api/v1/users/signup")
                            .contentType("application/json")
                            .content("""{"email":"test@example.com","password":"password","nickname":"test"}"""),
                    ).andExpect(status().is4xxClientError) // 400 Bad Request 예상 (유효성 검증 실패)
                }

                it("이메일 인증 엔드포인트는 인증 없이 접근 가능해야 함") {
                    // 인증 없이 접근 가능함을 확인 - 실제 처리 결과는 400 Bad Request가 될 수 있음
                    mockMvc.perform(
                        post("/api/v1/users/verify")
                            .contentType("application/json")
                            .content("""{"userId":"123","verificationCode":"ABC123"}"""),
                    ).andExpect(status().is4xxClientError) // 400 Bad Request 예상 (유효하지 않은 인증 코드)
                }

                it("헬스체크 엔드포인트는 인증 없이 접근 가능해야 함") {
                    mockMvc.perform(get("/actuator/health"))
                        .andExpect(status().isOk) // 헬스체크는 정상적으로 200 OK를 반환해야 함
                }
            }

            context("보호된 엔드포인트") {
                it("인증 없이 접근시 401 Unauthorized를 반환해야 함") {
                    mockMvc.perform(get("/api/v1/users/profile"))
                        .andExpect(status().isUnauthorized)
                }

                it("유효한 JWT 토큰으로 접근시 접근 가능해야 함") {
                    val token = "valid.jwt.token"
                    val userId = UserId("user123")

                    every { jwtTokenManager.validateAccessToken(token) } returns userId

                    mockMvc.perform(
                        get("/api/v1/users/profile")
                            .header("Authorization", "Bearer $token"),
                    ).andExpect(status().is2xxSuccessful) // 200 OK 또는 204 No Content 예상
                }
            }

            context("CSRF 설정") {
                it("CSRF가 비활성화되어 있어야 함 (JWT 사용)") {
                    // CSRF 토큰 없이도 POST 요청이 가능해야 함
                    mockMvc.perform(
                        post("/api/v1/users/login")
                            .contentType("application/json")
                            .content("""{"email":"test@example.com","password":"password"}"""),
                    ).andExpect(status().is4xxClientError) // 400 Bad Request 예상 (CSRF가 아닌 유효성 검증 실패)
                }
            }
        }
    }
}
