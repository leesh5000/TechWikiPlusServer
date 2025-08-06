package me.helloc.techwikiplus.service.user.e2e

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import me.helloc.techwikiplus.service.user.config.BaseE2eTest
import me.helloc.techwikiplus.service.user.config.annotations.E2eTest
import me.helloc.techwikiplus.service.user.interfaces.web.UserSignUpController
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
    fun `POST signup - 유효한 회원가입 데이터로 202 Accepted를 반환해야 한다`() {
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
            .andExpect(MockMvcResultMatchers.status().isAccepted)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
            .andExpect(MockMvcResultMatchers.content().string(""))
            .andDo(
                documentWithResource(
                    "user-signup-success",
                    ResourceSnippetParameters.builder()
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
                            Schema.schema(UserSignUpController.Request::class.java.simpleName),
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
            .andDo(
                documentWithResource(
                    "user-signup-invalid-email",
                    ResourceSnippetParameters.builder()
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
            .andDo(
                documentWithResource(
                    "user-signup-password-mismatch",
                    ResourceSnippetParameters.builder()
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
            .andDo(
                documentWithResource(
                    "user-signup-short-nickname",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 짧은 닉네임")
                        .description("닉네임이 2자 미만인 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST signup - 비밀번호가 약한 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserSignUpController.Request(
                email = "test@example.com",
                nickname = "테스터",
                password = "weak",
                confirmPassword = "weak",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andDo(
                documentWithResource(
                    "user-signup-weak-password",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 약한 비밀번호")
                        .description("비밀번호가 보안 요구사항(8-20자, 대소문자, 특수문자 포함)을 충족하지 않는 경우 400 Bad Request를 반환합니다.")
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
            .andExpect(MockMvcResultMatchers.status().isAccepted)

        // When & Then - 두 번째 회원가입 시도 (실패)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andDo(
                documentWithResource(
                    "user-signup-duplicate-email",
                    ResourceSnippetParameters.builder()
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
        val existingNickname = "기존닉네임"
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
            .andExpect(MockMvcResultMatchers.status().isAccepted)

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
            .andDo(
                documentWithResource(
                    "user-signup-duplicate-nickname",
                    ResourceSnippetParameters.builder()
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
            .andDo(
                documentWithResource(
                    "user-signup-missing-field",
                    ResourceSnippetParameters.builder()
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
            .andDo(
                documentWithResource(
                    "user-signup-invalid-nickname",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("사용자 회원가입 - 잘못된 닉네임 형식")
                        .description("닉네임에 허용되지 않는 특수문자가 포함된 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }
}
