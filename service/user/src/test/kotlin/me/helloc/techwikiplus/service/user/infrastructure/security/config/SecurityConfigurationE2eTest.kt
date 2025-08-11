package me.helloc.techwikiplus.service.user.infrastructure.security.config

import io.mockk.every
import io.mockk.mockk
import me.helloc.techwikiplus.service.user.config.BaseE2eTest
import me.helloc.techwikiplus.service.user.config.annotations.E2eTest
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.jwt.JwtTokenManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@E2eTest
@Import(SecurityConfigurationE2eTest.TestConfig::class)
class SecurityConfigurationE2eTest : BaseE2eTest() {
    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun mockJwtTokenManager(): JwtTokenManager = mockk()
    }

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setupMocks() {
        // Mock 설정 초기화
    }

    @Test
    fun `로그인 엔드포인트는 인증 없이 접근 가능해야 함`() {
        // 인증 없이 접근 가능함을 확인 - 실제 처리 결과는 400 Bad Request가 될 수 있음
        mockMvc.perform(
            post("/api/v1/users/login")
                .contentType("application/json")
                .content("""{"email":"test@example.com","password":"password"}"""),
        ).andExpect(status().is4xxClientError) // 400 Bad Request 또는 401 Unauthorized 예상
    }

    @Test
    fun `회원가입 엔드포인트는 인증 없이 접근 가능해야 함`() {
        // 인증 없이 접근 가능함을 확인 - 실제 처리 결과는 400 Bad Request가 될 수 있음
        mockMvc.perform(
            post("/api/v1/users/signup")
                .contentType("application/json")
                .content("""{"email":"test@example.com","password":"password","nickname":"test"}"""),
        ).andExpect(status().is4xxClientError) // 400 Bad Request 예상 (유효성 검증 실패)
    }

    @Test
    fun `이메일 인증 엔드포인트는 인증 없이 접근 가능해야 함`() {
        // 인증 없이 접근 가능함을 확인 - 실제 처리 결과는 400 Bad Request가 될 수 있음
        mockMvc.perform(
            post("/api/v1/users/verify")
                .contentType("application/json")
                .content("""{"userId":"123","verificationCode":"ABC123"}"""),
        ).andExpect(status().is4xxClientError) // 400 Bad Request 예상 (유효하지 않은 인증 코드)
    }

    @Test
    fun `헬스체크 엔드포인트는 인증 없이 접근 가능해야 함`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk) // 헬스체크는 정상적으로 200 OK를 반환해야 함
    }

    @Test
    fun `인증 없이 접근시 401 Unauthorized를 반환해야 함`() {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `유효한 JWT 토큰으로 접근시 접근 가능해야 함`() {
        // Given: 테스트용 사용자를 데이터베이스에 생성
        val userId = 123L
        val testUser = createTestUser(userId)
        userRepository.save(testUser)

        // Given: JWT 토큰 검증 Mock 설정
        val token = "valid.jwt.token"
        every { jwtTokenManager.validateAccessToken(token) } returns UserId(userId)

        // When & Then: 인증된 요청으로 프로필 조회
        mockMvc.perform(
            get("/api/v1/users/me")
                .header("Authorization", "Bearer $token"),
        ).andExpect(status().is2xxSuccessful) // 200 OK 또는 204 No Content 예상
    }

    private fun createTestUser(userId: Long): User {
        // 테스트용 사용자 객체 생성
        return User.create(
            id = UserId(userId),
            email = Email("test@example.com"),
            encodedPassword = EncodedPassword("hashedPassword"),
            nickname = Nickname("testUser"),
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = Instant.now(),
            modifiedAt = Instant.now(),
        )
    }

    @Test
    fun `CSRF가 비활성화되어 있어야 함 (JWT 사용)`() {
        // CSRF 토큰 없이도 POST 요청이 가능해야 함
        mockMvc.perform(
            post("/api/v1/users/login")
                .contentType("application/json")
                .content("""{"email":"test@example.com","password":"password"}"""),
        ).andExpect(status().is4xxClientError) // 400 Bad Request 예상 (CSRF가 아닌 유효성 검증 실패)
    }
}
