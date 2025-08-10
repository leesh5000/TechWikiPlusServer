package me.helloc.techwikiplus.service.user.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import me.helloc.techwikiplus.service.user.config.BaseE2eTest
import me.helloc.techwikiplus.service.user.config.annotations.E2eTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * UserSignUpController 통합 테스트
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
class UserSignUpControllerE2eTest : BaseE2eTest() {
    @Test
    fun `POST signup - 유효한 회원가입 데이터로 200 OK를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
            .andExpect(MockMvcResultMatchers.content().string(""))
            .andDo(
                documentWithResource(
                    "user-signup",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입")
                        .description(
                            """
                            새로운 사용자 계정을 생성합니다.
                            
                            회원가입이 성공하면 이메일 인증을 위한 인증 코드가 발송되며,
                            사용자는 /api/v1/users/verify 엔드포인트를 통해 이메일 인증을 완료해야 합니다.
                            """.trimIndent(),
                        )
                        .requestFields(
                            PayloadDocumentation.fieldWithPath("email")
                                .type(JsonFieldType.STRING)
                                .description("사용자 이메일 주소"),
                            PayloadDocumentation.fieldWithPath("nickname")
                                .type(JsonFieldType.STRING)
                                .description("사용자 닉네임 (2-20자)"),
                            PayloadDocumentation.fieldWithPath("password")
                                .type(JsonFieldType.STRING)
                                .description("비밀번호 (8-20자, 대소문자, 특수문자 포함)"),
                            PayloadDocumentation.fieldWithPath("confirmPassword")
                                .type(JsonFieldType.STRING)
                                .description("비밀번호 확인"),
                        )
                        .requestSchema(
                            Schema.Companion.schema(UserSignUpController.Request::class.java.simpleName),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 이메일 형식이 잘못된 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "invalid-email",
                nickname = "테스터",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_EMAIL_FORMAT"))
            .andDo(
                documentWithResource(
                    "user-signup-invalid-email",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 잘못된 이메일 형식")
                        .description("이메일 형식이 올바르지 않은 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 비밀번호가 일치하지 않는 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                password = "Test1234!",
                confirmPassword = "DifferentPassword1!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PASSWORD_MISMATCH"))
            .andDo(
                documentWithResource(
                    "user-signup-password-mismatch",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 비밀번호 불일치")
                        .description("비밀번호와 비밀번호 확인이 일치하지 않는 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 닉네임이 너무 짧은 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "a",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("NICKNAME_TOO_SHORT"))
            .andDo(
                documentWithResource(
                    "user-signup-short-nickname",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 짧은 닉네임")
                        .description("닉네임이 2자 미만인 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 비밀번호가 너무 짧은 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                // 6자 - 최소 8자 필요
                password = "Test1!",
                confirmPassword = "Test1!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PASSWORD_TOO_SHORT"))
            .andDo(
                documentWithResource(
                    "user-signup-short-password",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 짧은 비밀번호")
                        .description("비밀번호가 8자 미만인 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 비밀번호에 대문자가 없는 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                // 대문자 없음
                password = "test1234!",
                confirmPassword = "test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PASSWORD_NO_UPPERCASE"))
            .andDo(
                documentWithResource(
                    "user-signup-password-no-uppercase",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 대문자 없는 비밀번호")
                        .description("비밀번호에 대문자가 포함되지 않은 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 비밀번호에 소문자가 없는 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                // 소문자 없음
                password = "TEST1234!",
                confirmPassword = "TEST1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PASSWORD_NO_LOWERCASE"))
            .andDo(
                documentWithResource(
                    "user-signup-password-no-lowercase",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 소문자 없는 비밀번호")
                        .description("비밀번호에 소문자가 포함되지 않은 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 비밀번호에 특수문자가 없는 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                // 특수문자 없음
                password = "Test12345",
                confirmPassword = "Test12345",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PASSWORD_NO_SPECIAL_CHAR"))
            .andDo(
                documentWithResource(
                    "user-signup-password-no-special",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 특수문자 없는 비밀번호")
                        .description("비밀번호에 특수문자가 포함되지 않은 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 비밀번호가 너무 긴 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                // 34자 - 최대 30자
                password = "Test1234!" + "a".repeat(25),
                confirmPassword = "Test1234!" + "a".repeat(25),
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("PASSWORD_TOO_LONG"))
            .andDo(
                documentWithResource(
                    "user-signup-long-password",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 긴 비밀번호")
                        .description("비밀번호가 30자를 초과하는 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 이미 존재하는 이메일로 가입 시도하는 경우 409 Conflict를 반환해야 한다`() {
        // Given
        val existingEmail = "existing@example.com"
        val request =
            UserSignUpController.Request(
                email = existingEmail,
                nickname = "테스터",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // 첫 번째 회원가입 (성공)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        // When & Then - 두 번째 회원가입 시도 (실패)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("DUPLICATE_EMAIL"))
            .andDo(
                documentWithResource(
                    "user-signup-duplicate-email",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 중복 이메일")
                        .description("이미 등록된 이메일로 회원가입을 시도하는 경우 409 Conflict를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 이미 존재하는 닉네임으로 가입 시도하는 경우 409 Conflict를 반환해야 한다`() {
        // Given
        val existingNickname = "existingUser123"
        val firstRequest =
            UserSignUpController.Request(
                email = "first@example.com",
                nickname = existingNickname,
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // 첫 번째 회원가입 (성공)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        val secondRequest =
            UserSignUpController.Request(
                email = "second@example.com",
                nickname = existingNickname,
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then - 두 번째 회원가입 시도 (실패)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)),
        )
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("DUPLICATE_NICKNAME"))
            .andDo(
                documentWithResource(
                    "user-signup-duplicate-nickname",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 중복 닉네임")
                        .description("이미 사용 중인 닉네임으로 회원가입을 시도하는 경우 409 Conflict를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 필수 필드가 누락된 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val requestWithoutEmail =
            """
            {
                "nickname": "테스터",
                "password": "Test1234!",
                "confirmPassword": "Test1234!"
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestWithoutEmail),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MISSING_REQUIRED_FIELD"))
            .andDo(
                documentWithResource(
                    "user-signup-missing-field",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 필수 필드 누락")
                        .description(
                            "필수 필드(email, nickname, password, confirmPassword)가 누락된 경우 400 Bad Request를 반환합니다.",
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 닉네임에 특수문자가 포함된 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터@#$",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("NICKNAME_CONTAINS_SPECIAL_CHAR"))
            .andDo(
                documentWithResource(
                    "user-signup-invalid-nickname",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 잘못된 닉네임 형식")
                        .description("닉네임에 허용되지 않는 특수문자가 포함된 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 닉네임이 너무 긴 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                // 21자 - 최대 20자
                nickname = "가".repeat(21),
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("NICKNAME_TOO_LONG"))
            .andDo(
                documentWithResource(
                    "user-signup-long-nickname",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 긴 닉네임")
                        .description("닉네임이 20자를 초과하는 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 닉네임에 공백이 포함된 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터 공백",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("NICKNAME_CONTAINS_SPACE"))
            .andDo(
                documentWithResource(
                    "user-signup-nickname-with-space",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 공백 포함 닉네임")
                        .description("닉네임에 공백이 포함된 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 빈 닉네임인 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                // 공백만
                nickname = "   ",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("BLANK_NICKNAME"))
            .andDo(
                documentWithResource(
                    "user-signup-blank-nickname",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 빈 닉네임")
                        .description("닉네임이 공백만으로 이루어진 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 빈 이메일인 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                // 공백만
                email = "   ",
                nickname = "테스터",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("BLANK_EMAIL"))
            .andDo(
                documentWithResource(
                    "user-signup-blank-email",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 빈 이메일")
                        .description("이메일이 공백만으로 이루어진 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 닉네임이 정확히 2자인 경우 성공해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                // 정확히 2자 (최소값)
                nickname = "ab",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
    }

    @Test
    fun `POST signup - 닉네임이 정확히 20자인 경우 성공해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                // 정확히 20자 (최대값)
                nickname = "a".repeat(20),
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
    }

    @Test
    fun `POST signup - 비밀번호가 정확히 8자인 경우 성공해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                // 정확히 8자 (최소값)
                password = "Test123!",
                confirmPassword = "Test123!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
    }

    @Test
    fun `POST signup - 비밀번호가 정확히 30자인 경우 성공해야 한다`() {
        // Given
        val longPassword = "Test1!" + "a".repeat(24) // 정확히 30자 (최대값)
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                password = longPassword,
                confirmPassword = longPassword,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
    }

    @Test
    fun `POST signup - 한글 닉네임인 경우 성공해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "한글닉네임",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
    }

    @Test
    fun `POST signup - 영문 닉네임인 경우 성공해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "EnglishNick",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
    }

    @Test
    fun `POST signup - 숫자가 포함된 닉네임인 경우 성공해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "user123",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
    }

    @Test
    fun `POST signup - 언더스코어가 포함된 닉네임인 경우 성공해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "user_name",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
    }

    @Test
    fun `POST signup - 하이픈이 포함된 닉네임인 경우 성공해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "user-name",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
    }

    @Test
    fun `POST signup - Content-Type이 없는 경우 415 Unsupported Media Type을 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType)
    }

    @Test
    fun `POST signup - 잘못된 Content-Type인 경우 415 Unsupported Media Type을 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                password = "Test1234!",
                confirmPassword = "Test1234!",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType)
    }
}
