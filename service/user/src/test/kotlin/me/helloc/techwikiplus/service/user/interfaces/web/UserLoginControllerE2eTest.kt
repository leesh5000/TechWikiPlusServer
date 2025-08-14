package me.helloc.techwikiplus.service.user.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.service.common.infrastructure.id.SnowflakeUserIdGenerator
import me.helloc.techwikiplus.service.config.BaseE2eTest
import me.helloc.techwikiplus.service.config.annotations.E2eTest
import me.helloc.techwikiplus.service.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * UserLoginController 통합 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 실제 인증 로직 검증
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
class UserLoginControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var snowflakeIdGenerator: SnowflakeUserIdGenerator

    // BCrypt로 암호화된 "Password123!" 문자열 (대문자 P 포함)
    // DelegatingPasswordEncoder를 위해 {bcrypt} 접두사 추가
    private val encodedPassword = "{noop}Password123!"

    @Test
    fun `POST login - 유효한 로그인 정보로 200 OK와 토큰 정보를 반환해야 한다`() {
        // Given
        // Snowflake ID 충돌 방지
        val user = createActiveUser("logintest@example.com", "로그인테스터")

        val request =
            UserLoginController.Request(
                email = "logintest@example.com",
                password = "Password123!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.userId")
                    .value(user.id.toString()),
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.accessTokenExpiresAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.refreshTokenExpiresAt").exists())
            .andDo(
                documentWithResource(
                    "사용자 로그인 성공",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 로그인")
                        .description(
                            """
                            사용자 계정으로 로그인합니다.

                            로그인이 성공하면 액세스 토큰과 리프레시 토큰이 발급되며,
                            이후 인증이 필요한 API 호출 시 액세스 토큰을 사용해야 합니다.

                            액세스 토큰이 만료된 경우 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받을 수 있습니다.
                            """.trimIndent(),
                        )
                        .requestFields(
                            PayloadDocumentation.fieldWithPath("email")
                                .type(JsonFieldType.STRING)
                                .description("사용자 이메일 주소"),
                            PayloadDocumentation.fieldWithPath("password")
                                .type(JsonFieldType.STRING)
                                .description("사용자 비밀번호"),
                        )
                        .responseFields(
                            PayloadDocumentation.fieldWithPath("accessToken")
                                .type(JsonFieldType.STRING)
                                .description("액세스 토큰"),
                            PayloadDocumentation.fieldWithPath("refreshToken")
                                .type(JsonFieldType.STRING)
                                .description("리프레시 토큰"),
                            PayloadDocumentation.fieldWithPath("userId")
                                .type(JsonFieldType.STRING)
                                .description("사용자 고유 식별자"),
                            PayloadDocumentation.fieldWithPath("accessTokenExpiresAt")
                                .type(JsonFieldType.STRING)
                                .description("액세스 토큰 만료 시간 (ISO-8601 형식)"),
                            PayloadDocumentation.fieldWithPath("refreshTokenExpiresAt")
                                .type(JsonFieldType.STRING)
                                .description("리프레시 토큰 만료 시간 (ISO-8601 형식)"),
                        )
                        .requestSchema(
                            schema(
                                "${UserLoginController::class.simpleName}" +
                                    ".${UserLoginController.Request::class.simpleName}",
                            ),
                        )
                        .responseSchema(
                            schema(
                                "${UserLoginController::class.simpleName}" +
                                    ".${UserLoginController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login - 잘못된 비밀번호로 401 Unauthorized를 반환해야 한다`() {
        // Given
        // Snowflake ID 충돌 방지
        createActiveUser("wrongpass@example.com", "잘못된비밀번호")

        val request =
            UserLoginController.Request(
                email = "wrongpass@example.com",
                password = "WrongPassword!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("인증 정보가 올바르지 않습니다"))
            .andDo(
                documentWithResource(
                    "잘못된 비밀번호로 로그인",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 로그인 - 잘못된 비밀번호")
                        .description("비밀번호가 일치하지 않는 경우 401 Unauthorized를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginController::class.simpleName}" +
                                    ".${UserLoginController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login - 존재하지 않는 사용자로 401 Unauthorized를 반환해야 한다`() {
        // Given
        val request =
            UserLoginController.Request(
                email = "nonexistent@example.com",
                password = "Password123!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
            .andDo(
                documentWithResource(
                    "존재하지 않는 사용자 로그인",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 로그인 - 존재하지 않는 사용자")
                        .description("등록되지 않은 이메일로 로그인 시도하는 경우 404 Not Found를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginController::class.simpleName}" +
                                    ".${UserLoginController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login - BANNED 상태 사용자로 403 Forbidden을 반환해야 한다`() {
        // Given
        // Snowflake ID 충돌 방지
        createUserWithStatus("banned@example.com", "차단된사용자", UserStatus.BANNED)

        val request =
            UserLoginController.Request(
                email = "banned@example.com",
                password = "Password123!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_BANNED"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.message").value("차단된 계정입니다. 관리자에게 문의해주세요"),
            )
            .andDo(
                documentWithResource(
                    "차단된 사용자 로그인",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 로그인 - 차단된 사용자")
                        .description("BANNED 상태의 사용자가 로그인 시도하는 경우 403 Forbidden을 반환합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginController::class.simpleName}" +
                                    ".${UserLoginController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login - DELETED 상태 사용자로 401 Unauthorized를 반환해야 한다`() {
        // Given
        // Snowflake ID 충돌 방지
        createUserWithStatus("deleted@example.com", "삭제된사용자", UserStatus.DELETED)

        val request =
            UserLoginController.Request(
                email = "deleted@example.com",
                password = "Password123!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isGone)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_DELETED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 삭제된 계정입니다."))
            .andDo(
                documentWithResource(
                    "삭제된 사용자 로그인",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 로그인 - 삭제된 사용자")
                        .description("DELETED 상태의 사용자가 로그인 시도하는 경우 410 Gone을 반환합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginController::class.simpleName}" +
                                    ".${UserLoginController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login - DORMANT 상태 사용자로 423 Locked를 반환해야 한다`() {
        // Given
        // Snowflake ID 충돌 방지
        createUserWithStatus("dormant@example.com", "휴면사용자", UserStatus.DORMANT)

        val request =
            UserLoginController.Request(
                email = "dormant@example.com",
                password = "Password123!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_DORMANT"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.message").value("휴면 계정입니다. 관리자에게 문의해주세요"),
            )
            .andDo(
                documentWithResource(
                    "휴면 사용자 로그인",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 로그인 - 휴면 사용자")
                        .description("DORMANT 상태의 사용자가 로그인 시도하는 경우 403 Forbidden을 반혆합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginController::class.simpleName}" +
                                    ".${UserLoginController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login - PENDING 상태 사용자로 403 Forbidden을 반환해야 한다`() {
        // Given
        // Snowflake ID 충돌 방지
        createUserWithStatus("pending@example.com", "미인증사용자", UserStatus.PENDING)

        val request =
            UserLoginController.Request(
                email = "pending@example.com",
                password = "Password123!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_PENDING"))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.message",
                ).value("인증 대기중인 계정입니다. 이메일 인증을 완료 후 다시 시도해주세요."),
            )
            .andDo(
                documentWithResource(
                    "인증 대기중 사용자 로그인",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 로그인 - 미인증 사용자")
                        .description("PENDING 상태의 사용자가 로그인 시도하는 경우 403 Forbidden을 반환합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginController::class.simpleName}" +
                                    ".${UserLoginController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    private fun createActiveUser(
        email: String,
        nickname: String,
    ): User {
        return createUserWithStatus(email, nickname, UserStatus.ACTIVE)
    }

    private fun createUserWithStatus(
        email: String,
        nickname: String,
        status: UserStatus,
    ): User {
        val user =
            User.Companion.create(
                id = snowflakeIdGenerator.next(),
                email = Email(email),
                nickname = Nickname(nickname),
                encodedPassword = EncodedPassword(encodedPassword),
                status = status,
                role = UserRole.USER,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
            )

        return userRepository.save(user)
    }
}
