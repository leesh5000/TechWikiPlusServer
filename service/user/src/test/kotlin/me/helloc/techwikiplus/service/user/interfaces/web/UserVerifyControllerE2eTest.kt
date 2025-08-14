package me.helloc.techwikiplus.service.user.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.common.infrastructure.cache.RedisCacheStore
import me.helloc.techwikiplus.service.config.BaseE2eTest
import me.helloc.techwikiplus.service.config.annotations.E2eTest
import me.helloc.techwikiplus.service.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncryptor
import me.helloc.techwikiplus.service.user.domain.service.port.UserIdGenerator
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Duration
import java.time.Instant

/**
 * UserVerifyController 통합 테스트
 *
 * - 이메일 인증 기능 End-to-End 검증
 * - FIRST 원칙 준수 (Fast, Independent, Repeatable, Self-validating, Timely)
 * - 테스트 격리성 보장: 각 테스트는 독립적으로 실행 가능
 * - TestContainers를 통한 실제 DB 연동
 * - API 문서 자동 생성
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
    private lateinit var cacheStore: RedisCacheStore

    @Autowired
    private lateinit var userIdGenerator: UserIdGenerator

    @Autowired
    private lateinit var passwordEncryptor: PasswordEncryptor

    @Test
    fun `POST verify - 유효한 인증 코드로 201 Created를 반환해야 한다`() {
        // Given
        val email = "test@example.com"
        val verificationCode = "123456"
        val cacheKey = "registration_code:$email"

        // 사용자 생성 (PENDING 상태)
        val user = createPendingUser(email)
        userRepository.save(user)

        // 인증 코드 캐시에 저장 (10분)
        cacheStore.put(cacheKey, verificationCode, Duration.ofMinutes(10))

        val request =
            UserVerifyController.Request(
                email = email,
                registrationCode = verificationCode,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(
                MockMvcResultMatchers.header()
                    .string("Location", "/api/v1/users/login"),
            )
            .andExpect(MockMvcResultMatchers.content().string(""))
            .andDo(
                documentWithResource(
                    "이메일 인증 성공",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("이메일 인증")
                        .description(
                            """
                            회원가입 시 발송된 인증 코드를 통해 이메일을 인증합니다.
                            
                            인증이 성공하면 사용자 상태가 PENDING에서 ACTIVE로 변경되며,
                            로그인 페이지로 리다이렉트됩니다.
                            """.trimIndent(),
                        )
                        .requestFields(
                            PayloadDocumentation.fieldWithPath("email")
                                .type(JsonFieldType.STRING)
                                .description("인증할 이메일 주소"),
                            PayloadDocumentation.fieldWithPath("registrationCode")
                                .type(JsonFieldType.STRING)
                                .description("6자리 인증 코드"),
                        )
                        .requestSchema(
                            Schema.Companion.schema(
                                "${UserVerifyController::class.simpleName}" +
                                    ".${UserVerifyController.Request::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // 사용자 상태가 ACTIVE로 변경되었는지 검증
        val updatedUser = userRepository.findBy(user.id)
        updatedUser?.status shouldBe UserStatus.ACTIVE

        // 인증 코드가 캐시에서 삭제되었는지 검증
        cacheStore.get(cacheKey) shouldBe null
    }

    @Test
    fun `POST verify - 잘못된 인증 코드로 400 Bad Request를 반환해야 한다`() {
        // Given
        val email = "test@example.com"
        val correctCode = "123456"
        val wrongCode = "999999"
        val cacheKey = "registration_code:$email"

        // 사용자 생성 (PENDING 상태)
        val user = createPendingUser(email)
        userRepository.save(user)

        // 올바른 인증 코드 캐시에 저장
        cacheStore.put(cacheKey, correctCode, Duration.ofMinutes(10))

        val request =
            UserVerifyController.Request(
                email = email,
                // 잘못된 코드
                registrationCode = wrongCode,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("CODE_MISMATCH"),
            )
            .andDo(
                documentWithResource(
                    "잘못된 인증 코드로 인증",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 잘못된 인증 코드")
                        .description("인증 코드가 일치하지 않는 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            Schema.Companion.schema(
                                "${UserVerifyController::class.simpleName}" +
                                    ".${UserVerifyController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )

        // 사용자 상태가 변경되지 않았는지 검증
        val unchangedUser = userRepository.findBy(user.id)
        unchangedUser?.status shouldBe UserStatus.PENDING
    }

    @Test
    fun `POST verify - 만료된 인증 코드로 404 Not Found를 반환해야 한다`() {
        // Given
        val email = "test@example.com"
        val expiredCode = "123456"

        // 사용자 생성 (PENDING 상태)
        val user = createPendingUser(email)
        userRepository.save(user)

        // 인증 코드를 캐시에 저장하지 않음 (만료된 상태 시뮬레이션)

        val request =
            UserVerifyController.Request(
                email = email,
                registrationCode = expiredCode,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("REGISTRATION_EXPIRED"),
            )
            .andDo(
                documentWithResource(
                    "만료된 인증 코드로 인증",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 만료된 인증 코드")
                        .description("인증 코드가 만료된 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            Schema.Companion.schema(
                                "${UserVerifyController::class.simpleName}" +
                                    ".${UserVerifyController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 존재하지 않는 사용자로 404 Not Found를 반환해야 한다`() {
        // Given
        val nonExistentEmail = "nonexistent@example.com"
        val code = "123456"

        val request =
            UserVerifyController.Request(
                email = nonExistentEmail,
                registrationCode = code,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("USER_NOT_FOUND"),
            )
            .andDo(
                documentWithResource(
                    "존재하지 않는 사용자 인증",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 사용자 없음")
                        .description("등록되지 않은 이메일로 인증을 시도하는 경우 404 Not Found를 반환합니다.")
                        .requestSchema(
                            Schema.Companion.schema(
                                "${UserVerifyController::class.simpleName}" +
                                    ".${UserVerifyController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 이미 인증된 사용자로 요청하면 404 Not Found를 반환해야 한다`() {
        // Given
        val email = "active@example.com"
        val code = "123456"

        // 이미 활성화된 사용자 생성
        val activeUser = createActiveUser(email)
        userRepository.save(activeUser)

        val request =
            UserVerifyController.Request(
                email = email,
                registrationCode = code,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("NOT_FOUND_PENDING_USER"),
            )
            .andDo(
                documentWithResource(
                    "이미 인증된 사용자 재인증",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 이미 인증됨")
                        .description("이미 인증된 사용자가 재인증을 시도하는 경우 404 Not Found 반환합니다.")
                        .requestSchema(
                            Schema.Companion.schema(
                                "${UserVerifyController::class.simpleName}" +
                                    ".${UserVerifyController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 잘못된 이메일 형식으로 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserVerifyController.Request(
                // 잘못된 이메일 형식
                email = "invalid-email",
                registrationCode = "123456",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("INVALID_EMAIL_FORMAT"),
            )
            .andDo(
                documentWithResource(
                    "잘못된 이메일로 인증",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 잘못된 이메일 형식")
                        .description("이메일 형식이 올바르지 않은 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            Schema.Companion.schema(
                                "${UserVerifyController::class.simpleName}" +
                                    ".${UserVerifyController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 인증 코드가 5자리인 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val email = "test@example.com"

        // 사용자 생성
        val user = createPendingUser(email)
        userRepository.save(user)

        val request =
            UserVerifyController.Request(
                email = email,
                // 5자리 코드
                registrationCode = "12345",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("INVALID_ARGUMENT"),
            )
            .andDo(
                documentWithResource(
                    "잘못된 길이의 인증 코드",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 잘못된 코드 길이")
                        .description("인증 코드가 6자리가 아닌 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            Schema.Companion.schema(
                                "${UserVerifyController::class.simpleName}" +
                                    ".${UserVerifyController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 인증 코드가 7자리인 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val email = "test@example.com"

        // 사용자 생성
        val user = createPendingUser(email)
        userRepository.save(user)

        val request =
            UserVerifyController.Request(
                email = email,
                // 7자리 코드
                registrationCode = "1234567",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("INVALID_ARGUMENT"),
            )
    }

    @Test
    fun `POST verify - 인증 코드에 문자가 포함된 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val email = "test@example.com"

        // 사용자 생성
        val user = createPendingUser(email)
        userRepository.save(user)

        val request =
            UserVerifyController.Request(
                email = email,
                // 문자 포함
                registrationCode = "12345A",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("INVALID_ARGUMENT"),
            )
    }

    @Test
    fun `POST verify - 빈 이메일로 400 Bad Request를 반환해야 한다`() {
        // Given
        val request =
            UserVerifyController.Request(
                // 빈 이메일
                email = "",
                registrationCode = "123456",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("BLANK_EMAIL"),
            )
    }

    @Test
    fun `POST verify - 빈 인증 코드로 400 Bad Request를 반환해야 한다`() {
        // Given
        val email = "test@example.com"

        // 사용자 생성
        val user = createPendingUser(email)
        userRepository.save(user)

        val request =
            UserVerifyController.Request(
                email = email,
                // 빈 코드
                registrationCode = "",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("INVALID_ARGUMENT"),
            )
    }

    @Test
    fun `POST verify - 필수 필드가 누락된 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val requestWithoutCode =
            """
            {
                "email": "test@example.com"
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestWithoutCode),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("MISSING_REQUIRED_FIELD"),
            )
            .andDo(
                documentWithResource(
                    "필수 필드 누락으로 인증 실패",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User Management")
                        .summary("이메일 인증 - 필수 필드 누락")
                        .description(
                            "필수 필드(email, registrationCode)가 누락된 경우" +
                                " 400 Bad Request를 반환합니다.",
                        )
                        .requestSchema(
                            Schema.Companion.schema(
                                "${UserVerifyController::class.simpleName}" +
                                    ".${UserVerifyController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify - 동시에 같은 코드로 여러 번 인증 시도 시 첫 번째만 성공해야 한다`() {
        // Given
        val email = "test@example.com"
        val code = "123456"
        val cacheKey = "registration_code:$email"

        // 사용자 생성
        val user = createPendingUser(email)
        userRepository.save(user)

        // 인증 코드 저장
        cacheStore.put(cacheKey, code, Duration.ofMinutes(10))

        val request =
            UserVerifyController.Request(
                email = email,
                registrationCode = code,
            )

        // When - 첫 번째 요청 (성공)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        // Then - 두 번째 요청 (실패 - 이미 인증됨)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("NOT_FOUND_PENDING_USER"),
            )
    }

    @Test
    fun `POST verify - Content-Type이 없는 경우 415 Unsupported Media Type을 반환해야 한다`() {
        // Given
        val request =
            UserVerifyController.Request(
                email = "test@example.com",
                registrationCode = "123456",
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType)
    }

    @Test
    fun `POST verify - 차단된 사용자는 인증할 수 없어야 한다`() {
        // Given
        val email = "banned@example.com"
        val code = "123456"
        val cacheKey = "registration_code:$email"

        // 차단된 사용자 생성
        val bannedUser = createBannedUser(email)
        userRepository.save(bannedUser)

        // 인증 코드 저장
        cacheStore.put(cacheKey, code, Duration.ofMinutes(10))

        val request =
            UserVerifyController.Request(
                email = email,
                registrationCode = code,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("NOT_FOUND_PENDING_USER"),
            )
    }

    @Test
    fun `POST verify - 삭제된 사용자는 인증할 수 없어야 한다`() {
        // Given
        val email = "deleted@example.com"
        val code = "123456"
        val cacheKey = "registration_code:$email"

        // 삭제된 사용자 생성
        val deletedUser = createDeletedUser(email)
        userRepository.save(deletedUser)

        // 인증 코드 저장
        cacheStore.put(cacheKey, code, Duration.ofMinutes(10))

        val request =
            UserVerifyController.Request(
                email = email,
                registrationCode = code,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("NOT_FOUND_PENDING_USER"),
            )
    }

    // Helper Methods
    private fun createPendingUser(email: String): User {
        return User.Companion.create(
            id = userIdGenerator.next(),
            email = Email(email),
            encodedPassword =
                passwordEncryptor.encode(
                    RawPassword("Test1234!"),
                ),
            nickname = Nickname("testuser"),
            status = UserStatus.PENDING,
            role = UserRole.USER,
            createdAt = Instant.now(),
            modifiedAt = Instant.now(),
        )
    }

    private fun createActiveUser(email: String): User {
        return User.Companion.create(
            id = userIdGenerator.next(),
            email = Email(email),
            encodedPassword =
                passwordEncryptor.encode(
                    RawPassword("Test1234!"),
                ),
            nickname = Nickname("activeuser"),
            status = UserStatus.ACTIVE,
            role = UserRole.USER,
            createdAt = Instant.now(),
            modifiedAt = Instant.now(),
        )
    }

    private fun createBannedUser(email: String): User {
        return User.Companion.create(
            id = userIdGenerator.next(),
            email = Email(email),
            encodedPassword =
                passwordEncryptor.encode(
                    RawPassword("Test1234!"),
                ),
            nickname = Nickname("banneduser"),
            status = UserStatus.BANNED,
            role = UserRole.USER,
            createdAt = Instant.now(),
            modifiedAt = Instant.now(),
        )
    }

    private fun createDeletedUser(email: String): User {
        return User.Companion.create(
            id = userIdGenerator.next(),
            email = Email(email),
            encodedPassword =
                passwordEncryptor.encode(
                    RawPassword("Test1234!"),
                ),
            nickname = Nickname("deleteduser"),
            status = UserStatus.DELETED,
            role = UserRole.USER,
            createdAt = Instant.now(),
            modifiedAt = Instant.now(),
        )
    }
}
