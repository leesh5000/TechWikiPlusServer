package me.helloc.techwikiplus.service.user.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.config.BaseE2eTest
import me.helloc.techwikiplus.service.user.config.annotations.E2eTest
import me.helloc.techwikiplus.service.user.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.port.IdGenerator
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncryptor
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.service.user.infrastructure.cache.RedisCacheStore
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
 * UserVerifyResendController E2E 테스트
 *
 * - 이메일 인증 코드 재발송 기능 End-to-End 검증
 * - FIRST 원칙 준수 (Fast, Independent, Repeatable, Self-validating, Timely)
 * - 테스트 격리성 보장: 각 테스트는 독립적으로 실행 가능
 * - TestContainers를 통한 실제 DB/Redis 연동
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
class UserVerifyResendControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var cacheStore: RedisCacheStore

    @Autowired
    private lateinit var idGenerator: IdGenerator

    @Autowired
    private lateinit var passwordEncryptor: PasswordEncryptor

    @Test
    fun `POST verify-resend - PENDING 상태 사용자에게 인증 코드를 재발송하고 200 OK를 반환해야 한다`() {
        // Given
        val email = "test@example.com"
        val user = createPendingUser(email)
        userRepository.save(user)

        val request = UserVerifyResendController.Request(email = email)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))
            .andExpect(MockMvcResultMatchers.content().string(""))
            .andDo(
                documentWithResource(
                    "인증 코드 재발송 성공",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 코드 재발송")
                        .description(
                            """
                            이메일 인증 코드를 재발송합니다.
                            
                            인증 코드가 만료되었거나 받지 못한 경우 이 엔드포인트를 통해 
                            새로운 인증 코드를 요청할 수 있습니다.
                            새로운 코드가 발송되면 이전 코드는 무효화됩니다.
                            """.trimIndent(),
                        )
                        .requestFields(
                            PayloadDocumentation.fieldWithPath("email")
                                .type(JsonFieldType.STRING)
                                .description("인증 코드를 재발송할 이메일 주소"),
                        )
                        .requestSchema(
                            Schema.schema(
                                "${UserVerifyResendController::class.simpleName}" +
                                    ".${UserVerifyResendController.Request::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // 새로운 인증 코드가 캐시에 저장되었는지 검증
        val cacheKey = "registration_code:$email"
        val newCode = cacheStore.get(cacheKey)
        newCode shouldNotBe null
        newCode?.length shouldBe 6
        // 숫자로만 구성되어 있는지 검증
        newCode?.matches(Regex("\\d{6}")) shouldBe true
    }

    @Test
    fun `POST verify-resend - 이미 존재하는 인증 코드를 덮어쓰고 새 코드를 발송해야 한다`() {
        // Given
        val email = "test@example.com"
        val user = createPendingUser(email)
        userRepository.save(user)

        val cacheKey = "registration_code:$email"
        val oldCode = "123456"
        cacheStore.put(cacheKey, oldCode, Duration.ofMinutes(10))

        val request = UserVerifyResendController.Request(email = email)

        // When
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        // Then - 새로운 코드로 교체되었는지 검증
        val newCode = cacheStore.get(cacheKey)
        newCode shouldNotBe null
        newCode shouldNotBe oldCode // 기존 코드와 달라야 함
        newCode?.length shouldBe 6
    }

    @Test
    fun `POST verify-resend - 존재하지 않는 사용자로 404 Not Found를 반환해야 한다`() {
        // Given
        val nonExistentEmail = "nonexistent@example.com"
        val request = UserVerifyResendController.Request(email = nonExistentEmail)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
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
                    "존재하지 않는 사용자 재발송",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 코드 재발송 - 사용자 없음")
                        .description("등록되지 않은 이메일로 재발송을 요청하는 경우 404 Not Found를 반혆합니다.")
                        .requestSchema(
                            Schema.schema(
                                "${UserVerifyResendController::class.simpleName}" +
                                    ".${UserVerifyResendController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )

        // 캐시에 코드가 저장되지 않았는지 검증
        val cacheKey = "registration_code:$nonExistentEmail"
        cacheStore.get(cacheKey) shouldBe null
    }

    @Test
    fun `POST verify-resend - 이미 인증된 사용자로 404 Not Found를 반환해야 한다`() {
        // Given
        val email = "active@example.com"
        val activeUser = createActiveUser(email)
        userRepository.save(activeUser)

        val request = UserVerifyResendController.Request(email = email)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
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
                    "이미 인증된 사용자 재발송",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 코드 재발송 - 이미 인증됨")
                        .description("이미 인증된 사용자가 재발송을 요청하는 경우 404 Not Found를 반환합니다.")
                        .requestSchema(
                            Schema.schema(
                                "${UserVerifyResendController::class.simpleName}" +
                                    ".${UserVerifyResendController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify-resend - 차단된 사용자로 404 Not Found를 반환해야 한다`() {
        // Given
        val email = "banned@example.com"
        val bannedUser = createBannedUser(email)
        userRepository.save(bannedUser)

        val request = UserVerifyResendController.Request(email = email)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
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
    fun `POST verify-resend - 삭제된 사용자로 404 Not Found를 반환해야 한다`() {
        // Given
        val email = "deleted@example.com"
        val deletedUser = createDeletedUser(email)
        userRepository.save(deletedUser)

        val request = UserVerifyResendController.Request(email = email)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
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
    fun `POST verify-resend - 잘못된 이메일 형식으로 400 Bad Request를 반환해야 한다`() {
        // Given
        val request = UserVerifyResendController.Request(email = "invalid-email")

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
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
                    "잘못된 이메일로 재발송",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 코드 재발송 - 잘못된 이메일 형식")
                        .description("이메일 형식이 올바르지 않은 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            Schema.schema(
                                "${UserVerifyResendController::class.simpleName}" +
                                    ".${UserVerifyResendController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify-resend - 빈 이메일로 400 Bad Request를 반환해야 한다`() {
        // Given
        val request = UserVerifyResendController.Request(email = "")

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
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
    fun `POST verify-resend - 공백만 있는 이메일로 400 Bad Request를 반환해야 한다`() {
        // Given
        val request = UserVerifyResendController.Request(email = "   ")

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
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
    fun `POST verify-resend - 필수 필드가 누락된 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val requestWithoutEmail = "{}"

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestWithoutEmail),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("MISSING_REQUIRED_FIELD"),
            )
            .andDo(
                documentWithResource(
                    "필수 필드 누락으로 재발송 실패",
                    ResourceSnippetParameters.builder()
                        .tag("User Management")
                        .summary("이메일 인증 코드 재발송 - 필수 필드 누락")
                        .description("필수 필드(email)가 누락된 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            Schema.schema(
                                "${UserVerifyResendController::class.simpleName}" +
                                    ".${UserVerifyResendController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST verify-resend - 연속해서 재발송 요청 시 모두 성공해야 한다`() {
        // Given
        val email = "test@example.com"
        val user = createPendingUser(email)
        userRepository.save(user)

        val request = UserVerifyResendController.Request(email = email)
        val cacheKey = "registration_code:$email"

        // When - 첫 번째 재발송
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        val firstCode = cacheStore.get(cacheKey)

        // When - 두 번째 재발송
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        val secondCode = cacheStore.get(cacheKey)

        // Then
        firstCode shouldNotBe null
        secondCode shouldNotBe null
        firstCode shouldNotBe secondCode // 매번 새로운 코드 생성
    }

    @Test
    fun `POST verify-resend - Content-Type이 없는 경우 415 Unsupported Media Type을 반환해야 한다`() {
        // Given
        val request = UserVerifyResendController.Request(email = "test@example.com")

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType)
    }

    @Test
    fun `POST verify-resend - 대소문자가 다른 이메일로도 재발송이 가능해야 한다`() {
        // Given
        val originalEmail = "test@example.com"
        val user = createPendingUser(originalEmail)
        userRepository.save(user)

        // 대문자로 요청
        val request = UserVerifyResendController.Request(email = "TEST@EXAMPLE.COM")

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("Location", "/api/v1/users/verify"))

        // 코드가 정상적으로 저장되었는지 검증
        // Email 클래스가 이메일을 소문자로 정규화하므로 캐시 키도 소문자
        val cacheKey = "registration_code:test@example.com"
        val code = cacheStore.get(cacheKey)
        code shouldNotBe null
        code?.length shouldBe 6
    }

    @Test
    fun `POST verify-resend - 재발송 후 새 코드로 인증이 가능해야 한다`() {
        // Given
        val email = "test@example.com"
        val user = createPendingUser(email)
        userRepository.save(user)

        val resendRequest = UserVerifyResendController.Request(email = email)

        // 재발송 요청
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resendRequest)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        // 재발송된 코드 확인
        val cacheKey = "registration_code:$email"
        val newCode = cacheStore.get(cacheKey)

        // When - 새 코드로 인증 시도
        val verifyRequest =
            UserVerifyController.Request(
                email = email,
                registrationCode = newCode!!,
            )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        // Then - 사용자 상태가 ACTIVE로 변경되었는지 검증
        val updatedUser = userRepository.findBy(user.id)
        updatedUser?.status shouldBe UserStatus.ACTIVE
    }

    // Helper Methods
    private fun createPendingUser(email: String): User {
        return User.create(
            id = idGenerator.next(),
            email = Email(email),
            encodedPassword = passwordEncryptor.encode(RawPassword("Test1234!")),
            nickname = Nickname("pendinguser"),
            status = UserStatus.PENDING,
            role = UserRole.USER,
            createdAt = Instant.now(),
            modifiedAt = Instant.now(),
        )
    }

    private fun createActiveUser(email: String): User {
        return User.create(
            id = idGenerator.next(),
            email = Email(email),
            encodedPassword = passwordEncryptor.encode(RawPassword("Test1234!")),
            nickname = Nickname("activeuser"),
            status = UserStatus.ACTIVE,
            role = UserRole.USER,
            createdAt = Instant.now(),
            modifiedAt = Instant.now(),
        )
    }

    private fun createBannedUser(email: String): User {
        return User.create(
            id = idGenerator.next(),
            email = Email(email),
            encodedPassword = passwordEncryptor.encode(RawPassword("Test1234!")),
            nickname = Nickname("banneduser"),
            status = UserStatus.BANNED,
            role = UserRole.USER,
            createdAt = Instant.now(),
            modifiedAt = Instant.now(),
        )
    }

    private fun createDeletedUser(email: String): User {
        return User.create(
            id = idGenerator.next(),
            email = Email(email),
            encodedPassword = passwordEncryptor.encode(RawPassword("Test1234!")),
            nickname = Nickname("deleteduser"),
            status = UserStatus.DELETED,
            role = UserRole.USER,
            createdAt = Instant.now(),
            modifiedAt = Instant.now(),
        )
    }
}
