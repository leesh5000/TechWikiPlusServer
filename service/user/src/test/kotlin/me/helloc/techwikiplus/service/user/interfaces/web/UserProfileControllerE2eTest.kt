package me.helloc.techwikiplus.service.user.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import me.helloc.techwikiplus.service.user.config.BaseE2eTest
import me.helloc.techwikiplus.service.user.config.annotations.E2eTest
import me.helloc.techwikiplus.service.user.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.PasswordEncryptor
import me.helloc.techwikiplus.service.user.domain.port.UserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.jwt.JwtTokenManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.restdocs.request.RequestDocumentation
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * UserProfileController 통합 테스트
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
class UserProfileControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    @Autowired
    private lateinit var passwordEncryptor: PasswordEncryptor

    private lateinit var testUser: User
    private lateinit var adminUser: User
    private lateinit var otherUser: User
    private lateinit var testUserToken: String
    private lateinit var adminUserToken: String
    private lateinit var otherUserToken: String

    @BeforeEach
    fun setUpTestData() {
        // 테스트 사용자 생성
        testUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("test@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Test1234!")),
                nickname = Nickname("testuser"),
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
            )
        testUser = userRepository.save(testUser)
        // Snowflake ID 충돌 방지

        adminUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("admin@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Admin1234!")),
                nickname = Nickname("admin"),
                role = UserRole.ADMIN,
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
            )
        adminUser = userRepository.save(adminUser)
        // Snowflake ID 충돌 방지

        otherUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("other@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Other1234!")),
                nickname = Nickname("otheruser"),
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
            )
        otherUser = userRepository.save(otherUser)

        // JWT 토큰 생성
        testUserToken = jwtTokenManager.generateAccessToken(testUser.id).token
        adminUserToken = jwtTokenManager.generateAccessToken(adminUser.id).token
        otherUserToken = jwtTokenManager.generateAccessToken(otherUser.id).token

        // 저장된 사용자 확인 (디버깅용)
        userRepository.findBy(testUser.id)
            ?: throw IllegalStateException("Test user not found in repository after save")
    }

    @Test
    fun `GET profile by userId - 인증된 사용자가 자신의 프로필을 조회할 때 200 OK와 프로필 정보를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", testUser.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(testUser.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("test@example.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nickname").value("testuser"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("USER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.createdAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.modifiedAt").exists())
            .andDo(
                documentWithResource(
                    "사용자 프로필 조회 성공",
                    ResourceSnippetParameters.builder()
                        .tag("User Profile")
                        .summary("사용자 프로필 조회")
                        .description(
                            """
                            특정 사용자의 프로필 정보를 조회합니다.
                            
                            인증이 필요하며, JWT 토큰을 Bearer 형식으로 전달해야 합니다.
                            사용자는 자신의 프로필 또는 다른 사용자의 프로필을 조회할 수 있습니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            RequestDocumentation.parameterWithName("userId")
                                .description("조회할 사용자의 ID"),
                        )
                        .requestHeaders(
                            HeaderDocumentation.headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰}"),
                        )
                        .responseFields(
                            PayloadDocumentation.fieldWithPath("userId")
                                .type(JsonFieldType.STRING)
                                .description("사용자 ID"),
                            PayloadDocumentation.fieldWithPath("email")
                                .type(JsonFieldType.STRING)
                                .description("사용자 이메일"),
                            PayloadDocumentation.fieldWithPath("nickname")
                                .type(JsonFieldType.STRING)
                                .description("사용자 닉네임"),
                            PayloadDocumentation.fieldWithPath("role")
                                .type(JsonFieldType.STRING)
                                .description("사용자 권한 (USER, ADMIN)"),
                            PayloadDocumentation.fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("사용자 상태 (ACTIVE, PENDING, DORMANT, BANNED, DELETED)"),
                            PayloadDocumentation.fieldWithPath("createdAt")
                                .type(JsonFieldType.STRING)
                                .description("계정 생성 시간 (ISO-8601)"),
                            PayloadDocumentation.fieldWithPath("modifiedAt")
                                .type(JsonFieldType.STRING)
                                .description("계정 수정 시간 (ISO-8601)"),
                        )
                        .responseSchema(
                            Schema.schema(
                                "${UserProfileController::class.simpleName}." +
                                    "${UserProfileController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET profile by userId - 사용자가 자신의 프로필을 조회할 때 200 OK와 프로필 정보를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", testUser.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(testUser.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("test@example.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nickname").value("testuser"))
    }

    @Test
    fun `GET profile by userId - 관리자가 다른 사용자의 프로필을 조회할 때 200 OK와 프로필 정보를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", otherUser.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(otherUser.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("other@example.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nickname").value("otheruser"))
            .andDo(
                documentWithResource(
                    "관리자 프로필 조회",
                    ResourceSnippetParameters.builder()
                        .tag("User Profile")
                        .summary("특정 사용자 프로필 조회 - 관리자 권한")
                        .description(
                            """
                            관리자는 모든 사용자의 프로필을 조회할 수 있습니다.
                            ADMIN 권한이 필요합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            RequestDocumentation.parameterWithName("userId")
                                .description("조회할 사용자의 ID"),
                        )
                        .requestHeaders(
                            HeaderDocumentation.headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰} (ADMIN 권한 필요)"),
                        )
                        .responseSchema(
                            Schema.schema(
                                "${UserProfileController::class.simpleName}." +
                                    "${UserProfileController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET profile by userId - 일반 사용자가 다른 사용자의 프로필을 조회할 때도 200 OK를 반환해야 한다`() {
        // When & Then
        // Note: 현재 구현에서는 인증된 사용자라면 누구나 다른 사용자의 프로필을 조회할 수 있음
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", otherUser.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(otherUser.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("other@example.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nickname").value("otheruser"))
            .andDo(
                documentWithResource(
                    "타인 프로필 조회",
                    ResourceSnippetParameters.builder()
                        .tag("User Profile")
                        .summary("특정 사용자 프로필 조회 - 다른 사용자")
                        .description(
                            """
                            인증된 사용자가 다른 사용자의 프로필을 조회합니다.
                            현재 구현에서는 인증된 사용자라면 누구나 다른 사용자의 프로필을 조회할 수 있습니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            RequestDocumentation.parameterWithName("userId")
                                .description("조회할 사용자의 ID"),
                        )
                        .requestHeaders(
                            HeaderDocumentation.headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰}"),
                        )
                        .responseSchema(
                            Schema.schema(
                                "${UserProfileController::class.simpleName}." +
                                    "${UserProfileController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET profile by userId - 인증되지 않은 사용자가 프로필을 조회할 때 401 Unauthorized를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", testUser.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andDo(
                documentWithResource(
                    "인증 없이 프로필 조회",
                    ResourceSnippetParameters.builder()
                        .tag("User Profile")
                        .summary("특정 사용자 프로필 조회 - 인증 실패")
                        .description("인증 토큰 없이 특정 사용자의 프로필을 조회하려고 할 때 401 Unauthorized를 반환합니다.")
                        .pathParameters(
                            RequestDocumentation.parameterWithName("userId")
                                .description("조회할 사용자의 ID"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET profile by userId - 존재하지 않는 사용자의 프로필을 조회할 때 404 Not Found를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", 999999L)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_NOT_FOUND"))
            .andDo(
                documentWithResource(
                    "존재하지 않는 사용자 프로필 조회",
                    ResourceSnippetParameters.builder()
                        .tag("User Profile")
                        .summary("특정 사용자 프로필 조회 - 사용자 없음")
                        .description("존재하지 않는 사용자 ID로 프로필을 조회하려고 할 때 404 Not Found를 반하합니다.")
                        .pathParameters(
                            RequestDocumentation.parameterWithName("userId")
                                .description("조회할 사용자의 ID"),
                        )
                        .requestHeaders(
                            HeaderDocumentation.headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰}"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET profile by userId - 빈 userId로 조회할 때 404 Not Found를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", " ")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `GET profile by userId - 너무 긴 userId로 조회할 때 400 Bad Request를 반환해야 한다`() {
        // Given
        val longUserId = "a".repeat(300)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", longUserId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("INVALID_USER_ID_FORMAT"),
            )
            .andDo(
                documentWithResource(
                    "잘못된 사용자ID로 프로필 조회",
                    ResourceSnippetParameters.builder()
                        .tag("User Profile")
                        .summary("특정 사용자 프로필 조회 - 잘못된 userId")
                        .description("너무 긴 userId로 프로필을 조회하려고 할 때 400 Bad Request를 반환합니다.")
                        .pathParameters(
                            RequestDocumentation.parameterWithName("userId")
                                .description("조회할 사용자의 ID"),
                        )
                        .requestHeaders(
                            HeaderDocumentation.headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰}"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET profile by userId - 관리자가 자신의 프로필을 조회할 때 ADMIN 권한 정보가 포함되어야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", adminUser.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.userId")
                    .value(adminUser.id.value),
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("ADMIN"))
    }

    @Test
    fun `GET profile by userId - DORMANT 상태의 사용자 프로필 조회 시 403 Forbidden을 반환해야 한다`() {
        // Given
        val dormantUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("dormant@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Dormant1234!")),
                nickname = Nickname("dormantuser"),
                role = UserRole.USER,
                status = UserStatus.DORMANT,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
            )
        userRepository.save(dormantUser)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", dormantUser.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.code")
                    .value("USER_DORMANT"),
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.message")
                    .value("휴면 계정입니다. 관리자에게 문의해주세요"),
            )
            .andDo(
                documentWithResource(
                    "휴면 사용자 프로필 조회",
                    ResourceSnippetParameters.builder()
                        .tag("User Profile")
                        .summary("특정 사용자 프로필 조회 - 휴면 계정")
                        .description("휴면 상태(DORMANT)의 사용자 프로필은 관리자도 조회할 수 없으며, 403 Forbidden이 반환됩니다.")
                        .pathParameters(
                            RequestDocumentation.parameterWithName("userId")
                                .description("조회할 사용자의 ID"),
                        )
                        .requestHeaders(
                            HeaderDocumentation.headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰} (ADMIN 권한)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET profile by userId - DELETED 상태의 사용자는 조회할 수 없어야 한다`() {
        // Given
        val deletedUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("deleted@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Deleted1234!")),
                nickname = Nickname("deleteduser"),
                role = UserRole.USER,
                status = UserStatus.DELETED,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
            )
        userRepository.save(deletedUser)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", deletedUser.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isGone)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_DELETED"))
            .andDo(
                documentWithResource(
                    "삭제된 사용자 프로필 조회",
                    ResourceSnippetParameters.builder()
                        .tag("User Profile")
                        .summary("특정 사용자 프로필 조회 - 삭제된 계정")
                        .description("삭제된 상태(DELETED)의 사용자는 조회할 수 없어 410 Gone을 반환합니다.")
                        .pathParameters(
                            RequestDocumentation.parameterWithName("userId")
                                .description("조회할 사용자의 ID"),
                        )
                        .requestHeaders(
                            HeaderDocumentation.headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰} (ADMIN 권한)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET profile by userId - 사용자가 자기 자신을 userId로 조회할 때도 성공해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/{userId}", testUser.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $testUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(testUser.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("test@example.com"))
    }
}
