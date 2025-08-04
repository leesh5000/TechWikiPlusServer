package me.helloc.techwikiplus.service.user.e2e

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import me.helloc.techwikiplus.service.user.config.BaseE2eTest
import me.helloc.techwikiplus.service.user.config.annotations.E2eTest
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import me.helloc.techwikiplus.service.user.domain.service.port.IdGenerator
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore
import me.helloc.techwikiplus.service.user.interfaces.UserVerifyController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * UserVerifyController 통합 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB/Redis 연동
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
class UserVerifyControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var verificationCodeStore: VerificationCodeStore

    @Autowired
    private lateinit var clockHolder: ClockHolder

    @Autowired
    private lateinit var idGenerator: IdGenerator

    @Test
    fun `POST verify - 유효한 이메일과 인증 코드로 201 Created를 반환해야 한다`() {
        // Snowflake ID 생성 시간 충돌 방지를 위한 짧은 대기
        Thread.sleep(10)
        // Given - PENDING 상태의 사용자 생성
        val email = Email("test@example.com")
        val verificationCode = VerificationCode("123456")
        val now = clockHolder.now()

        val pendingUser =
            User(
                id = idGenerator.next(),
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("테스터"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        userRepository.save(pendingUser)

        // 인증 코드를 캐시에 저장
        verificationCodeStore.store(email, verificationCode)

        val request =
            UserVerifyController.Request(
                email = email.value,
                verificationCode = verificationCode.value,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/login"))
            .andExpect(MockMvcResultMatchers.content().string(""))
            .andDo(
                documentWithResource(
                    "user-verify-success",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증")
                        .description(
                            """
                            사용자의 이메일 주소를 인증합니다.
                            
                            회원가입 시 발송된 인증 코드를 사용하여 이메일을 인증하면,
                            사용자 상태가 PENDING에서 ACTIVE로 변경되며 로그인이 가능해집니다.
                            """.trimIndent(),
                        )
                        .requestFields(
                            PayloadDocumentation.fieldWithPath("email")
                                .type(JsonFieldType.STRING)
                                .description("인증할 이메일 주소"),
                            PayloadDocumentation.fieldWithPath("verificationCode")
                                .type(JsonFieldType.STRING)
                                .description("6자리 인증 코드"),
                        )
                        .requestSchema(
                            Schema.schema(UserVerifyController.Request::class.java.simpleName),
                        )
                        .build(),
                ),
            )

        // 사용자 상태가 ACTIVE로 변경되었는지 확인
        val activatedUser = userRepository.findBy(email)
        assert(activatedUser != null)
        assert(activatedUser!!.status == UserStatus.ACTIVE)
    }

    @Test
    fun `POST verify - 존재하지 않는 이메일로 인증 시도 시 404 Not Found를 반환해야 한다`() {
        // Given - 캐시에만 인증 코드 저장 (사용자는 없음)
        val email = Email("nonexistent@example.com")
        val verificationCode = VerificationCode("123456")
        verificationCodeStore.store(email, verificationCode)

        val request =
            UserVerifyController.Request(
                email = email.value,
                verificationCode = verificationCode.value,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
            .andDo(
                documentWithResource(
                    "user-verify-user-not-found",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 사용자 없음")
                        .description("존재하지 않는 이메일로 인증을 시도하는 경우 404 Not Found를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 잘못된 인증 코드로 인증 시도 시 400 Bad Request를 반환해야 한다`() {
        // Given - 사용자는 있지만 다른 코드를 캐시에 저장
        val email = Email("test@example.com")
        val correctCode = VerificationCode("123456")
        val wrongCode = VerificationCode("999999")
        val now = clockHolder.now()

        val pendingUser =
            User(
                id = idGenerator.next(),
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("테스터"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        userRepository.save(pendingUser)

        // 올바른 코드를 캐시에 저장
        verificationCodeStore.store(email, correctCode)

        val request =
            UserVerifyController.Request(
                email = email.value,
                // 잘못된 코드 사용
                verificationCode = wrongCode.value,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid verification code"))
            .andDo(
                documentWithResource(
                    "user-verify-invalid-code",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 잘못된 인증 코드")
                        .description("잘못된 인증 코드로 인증을 시도하는 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 캐시에 인증 코드가 없는 경우 400 Bad Request를 반환해야 한다`() {
        // Snowflake ID 생성 시간 충돌 방지를 위한 짧은 대기
        Thread.sleep(10)
        // Given - 사용자는 있지만 캐시에 코드 없음 (만료된 상황)
        val email = Email("test@example.com")
        val verificationCode = VerificationCode("123456")
        val now = clockHolder.now()

        val pendingUser =
            User(
                id = idGenerator.next(),
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("테스터"),
                status = UserStatus.PENDING,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        userRepository.save(pendingUser)

        // 캐시에 코드를 저장하지 않음 (만료된 상황 시뮬레이션)

        val request =
            UserVerifyController.Request(
                email = email.value,
                verificationCode = verificationCode.value,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
            .andDo(
                documentWithResource(
                    "user-verify-code-not-found",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 인증 코드 없음")
                        .description("인증 코드가 만료되었거나 캐시에 없는 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 이메일 형식이 잘못된 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserVerifyController.Request(
                email = "invalid-email",
                verificationCode = "123456",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].field").value("email"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].code").value("INVALID_EMAIL_FORMAT"))
            .andDo(
                documentWithResource(
                    "user-verify-invalid-email",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 잘못된 이메일 형식")
                        .description("이메일 형식이 올바르지 않은 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 인증 코드가 6자리가 아닌 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserVerifyController.Request(
                email = "test@example.com",
                // 5자리
                verificationCode = "12345",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_ARGUMENT"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("인증 코드는 정확히 6자리여야 합니다"))
            .andDo(
                documentWithResource(
                    "user-verify-invalid-code-length",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 잘못된 인증 코드 길이")
                        .description("인증 코드가 6자리가 아닌 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 필수 필드가 누락된 경우 400 Bad Request를 반환해야 한다`() {
        // Given - 이메일 필드 누락
        val requestWithoutEmail =
            """
            {
                "verificationCode": "123456"
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestWithoutEmail),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andDo(
                documentWithResource(
                    "user-verify-missing-field",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 필수 필드 누락")
                        .description("필수 필드(email, verificationCode)가 누락된 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 이미 활성화된 사용자가 인증 시도 시 404 Not Found를 반환해야 한다`() {
        // Given - ACTIVE 상태의 사용자
        val email = Email("active@example.com")
        val verificationCode = VerificationCode("123456")
        val now = clockHolder.now()

        val activeUser =
            User(
                id = idGenerator.next(),
                email = email,
                encodedPassword = EncodedPassword("encodedPassword"),
                nickname = Nickname("활성사용자"),
                // 이미 활성화된 상태
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        userRepository.save(activeUser)

        // 인증 코드를 캐시에 저장
        verificationCodeStore.store(email, verificationCode)

        val request =
            UserVerifyController.Request(
                email = email.value,
                verificationCode = verificationCode.value,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
            .andDo(
                documentWithResource(
                    "user-verify-already-active",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 이미 활성화된 사용자")
                        .description("이미 활성화된 사용자가 인증을 시도하는 경우에는 404 Not Found를 반환합니다.")
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 인증 코드에 숫자가 아닌 문자가 포함된 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserVerifyController.Request(
                email = "test@example.com",
                // 문자 포함
                verificationCode = "12345A",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_ARGUMENT"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("인증 코드는 숫자로만 구성되어야 합니다"))
            .andDo(
                documentWithResource(
                    "user-verify-invalid-code-format",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 잘못된 인증 코드 형식")
                        .description("인증 코드에 숫자가 아닌 문자가 포함된 경우 400 Bad Request를 반환합니다.")
                        .build(),
                ),
            )
    }
}
