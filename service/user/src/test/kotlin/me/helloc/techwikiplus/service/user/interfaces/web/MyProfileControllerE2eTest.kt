package me.helloc.techwikiplus.service.user.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters.Companion.builder
import com.epages.restdocs.apispec.Schema.Companion.schema
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
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * MyProfileController E2E 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - JWT 기반 인증 테스트
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
class MyProfileControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    @Autowired
    private lateinit var passwordEncryptor: PasswordEncryptor

    private lateinit var activeUser: User
    private lateinit var adminUser: User
    private lateinit var pendingUser: User
    private lateinit var dormantUser: User
    private lateinit var bannedUser: User
    private lateinit var deletedUser: User

    private lateinit var activeUserToken: String
    private lateinit var adminUserToken: String
    private lateinit var pendingUserToken: String
    private lateinit var dormantUserToken: String
    private lateinit var bannedUserToken: String
    private lateinit var expiredToken: String
    private lateinit var invalidToken: String

    @BeforeEach
    fun setUpTestData() {
        // ACTIVE 상태 일반 사용자
        activeUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("active@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Active1234!")),
                nickname = Nickname("activeuser"),
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
                // 1일 전
                createdAt = Instant.now().minusSeconds(86400),
                // 1시간 전
                modifiedAt = Instant.now().minusSeconds(3600),
            )
        activeUser = userRepository.save(activeUser)
        // Snowflake ID 충돌 방지

        // ADMIN 권한 사용자
        adminUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("admin@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Admin1234!")),
                nickname = Nickname("adminuser"),
                role = UserRole.ADMIN,
                status = UserStatus.ACTIVE,
                // 2일 전
                createdAt = Instant.now().minusSeconds(172800),
                modifiedAt = Instant.now(),
            )
        adminUser = userRepository.save(adminUser)

        // PENDING 상태 사용자 (이메일 미인증)
        pendingUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("pending@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Pending1234!")),
                nickname = Nickname("pendinguser"),
                role = UserRole.USER,
                status = UserStatus.PENDING,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
            )
        pendingUser = userRepository.save(pendingUser)

        // DORMANT 상태 사용자 (휴면 계정)
        dormantUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("dormant@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Dormant1234!")),
                nickname = Nickname("dormantuser"),
                role = UserRole.USER,
                status = UserStatus.DORMANT,
                // 1년 전
                createdAt = Instant.now().minusSeconds(31536000),
                // 90일 전
                modifiedAt = Instant.now().minusSeconds(7776000),
            )
        dormantUser = userRepository.save(dormantUser)

        // BANNED 상태 사용자 (정지된 계정)
        bannedUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("banned@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Banned1234!")),
                nickname = Nickname("banneduser"),
                role = UserRole.USER,
                status = UserStatus.BANNED,
                // 7일 전
                createdAt = Instant.now().minusSeconds(604800),
                modifiedAt = Instant.now(),
            )
        bannedUser = userRepository.save(bannedUser)

        // DELETED 상태 사용자 (삭제된 계정)
        deletedUser =
            User(
                id = UserId(System.currentTimeMillis()),
                email = Email("deleted@example.com"),
                encodedPassword = passwordEncryptor.encode(RawPassword("Deleted1234!")),
                nickname = Nickname("deleteduser"),
                role = UserRole.USER,
                status = UserStatus.DELETED,
                // 30일 전
                createdAt = Instant.now().minusSeconds(2592000),
                modifiedAt = Instant.now(),
            )
        deletedUser = userRepository.save(deletedUser)

        // JWT 토큰 생성
        activeUserToken = jwtTokenManager.generateAccessToken(activeUser.id).token
        adminUserToken = jwtTokenManager.generateAccessToken(adminUser.id).token
        pendingUserToken = jwtTokenManager.generateAccessToken(pendingUser.id).token
        dormantUserToken = jwtTokenManager.generateAccessToken(dormantUser.id).token
        bannedUserToken = jwtTokenManager.generateAccessToken(bannedUser.id).token

        // 만료된 토큰 (실제 구현에서는 시간 조작이 필요하므로 임시로 잘못된 토큰 사용)
        expiredToken =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTYiLCJpYXQiOjE2MDk0NTkyMDAsImV4cCI6MTYwOTQ1OTIwMH0.expired"

        // 잘못된 형식의 토큰
        invalidToken = "invalid.token.format"
    }

    @Test
    fun `GET me - 인증된 일반 사용자가 자신의 프로필을 조회할 때 200 OK와 프로필 정보를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $activeUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(activeUser.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("active@example.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nickname").value("activeuser"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("USER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.createdAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.modifiedAt").exists())
            .andDo(
                documentWithResource(
                    "내 프로필 조회 성공",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회")
                        .description(
                            """
                            현재 로그인한 사용자의 프로필 정보를 조회합니다.
                            
                            인증이 필요하며, JWT 토큰을 Bearer 형식으로 전달해야 합니다.
                            토큰에서 추출한 사용자 ID를 기반으로 프로필 정보를 반환합니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 액세스 토큰}")
                                .optional(),
                        )
                        .responseFields(
                            fieldWithPath("userId")
                                .type(JsonFieldType.STRING)
                                .description("사용자 고유 ID (Snowflake ID)"),
                            fieldWithPath("email")
                                .type(JsonFieldType.STRING)
                                .description("사용자 이메일 주소"),
                            fieldWithPath("nickname")
                                .type(JsonFieldType.STRING)
                                .description("사용자 닉네임"),
                            fieldWithPath("role")
                                .type(JsonFieldType.STRING)
                                .description("사용자 권한 (USER, ADMIN)"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("계정 상태 (ACTIVE, PENDING, DORMANT, BANNED, DELETED)"),
                            fieldWithPath("createdAt")
                                .type(JsonFieldType.STRING)
                                .description("계정 생성 시간 (ISO-8601 형식)"),
                            fieldWithPath("modifiedAt")
                                .type(JsonFieldType.STRING)
                                .description("계정 최종 수정 시간 (ISO-8601 형식)"),
                        )
                        .responseSchema(
                            schema(
                                "${MyProfileController::class.simpleName}." +
                                    "${MyProfileController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - ADMIN 권한 사용자가 자신의 프로필을 조회할 때 ADMIN role이 포함되어야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(adminUser.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("admin@example.com"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nickname").value("adminuser"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("ADMIN"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"))
            .andDo(
                documentWithResource(
                    "관리자 프로필 조회",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회 - 관리자")
                        .description(
                            """
                            관리자 권한을 가진 사용자가 자신의 프로필을 조회합니다.
                            role 필드에 'ADMIN'이 반환됩니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 액세스 토큰} (ADMIN 권한)"),
                        )
                        .responseSchema(
                            schema(
                                "${MyProfileController::class.simpleName}." +
                                    "${MyProfileController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - PENDING 상태 사용자는 자신의 프로필을 조회할 수 없다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $pendingUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_PENDING"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
            .andDo(
                documentWithResource(
                    "미인증 사용자 프로필 조회 실패",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회 - 이메일 미인증 사용자")
                        .description(
                            """
                            이메일 인증을 완료하지 않은 PENDING 상태의 사용자가 
                            프로필을 조회하려고 할 때 403 Forbidden을 반환합니다.
                            
                            이메일 인증을 완료해야 정상적으로 서비스를 이용할 수 있습니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 액세스 토큰} (PENDING 상태)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - 인증 헤더 없이 요청할 때 401 Unauthorized를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andDo(
                documentWithResource(
                    "인증 없이 프로필 조회",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회 - 인증 실패")
                        .description(
                            """
                            인증 토큰 없이 프로필을 조회하려고 할 때 401 Unauthorized를 반환합니다.
                            
                            이 엔드포인트는 반드시 인증이 필요합니다.
                            """.trimIndent(),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - Bearer 없이 토큰만 전송할 때 401 Unauthorized를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, activeUserToken) // Bearer 없이 토큰만
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andDo(
                documentWithResource(
                    "잘못된 인증 헤더 형식",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회 - 잘못된 토큰 형식")
                        .description(
                            """
                            Authorization 헤더에 'Bearer' 접두사 없이 토큰만 전송한 경우
                            401 Unauthorized를 반환합니다.
                            
                            올바른 형식: Authorization: Bearer {token}
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("토큰만 전송 (Bearer 누락)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - 잘못된 형식의 토큰으로 요청할 때 401 Unauthorized를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $invalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andDo(
                documentWithResource(
                    "잘못된 토큰으로 프로필 조회",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회 - 유효하지 않은 토큰")
                        .description(
                            """
                            JWT 형식이 아닌 잘못된 토큰으로 요청한 경우
                            401 Unauthorized를 반환합니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {잘못된 형식의 토큰}"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - 만료된 토큰으로 요청할 때 401 Unauthorized를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andDo(
                documentWithResource(
                    "만료된 토큰으로 프로필 조회",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회 - 만료된 토큰")
                        .description(
                            """
                            만료된 JWT 토큰으로 요청한 경우 401 Unauthorized를 반환합니다.
                            
                            토큰이 만료된 경우 /api/v1/users/refresh 엔드포인트를 통해
                            새로운 액세스 토큰을 발급받아야 합니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {만료된 JWT 토큰}"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - DORMANT 상태 사용자가 자신의 프로필을 조회할 때 403 Forbidden을 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $dormantUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_DORMANT"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("휴면 계정입니다. 관리자에게 문의해주세요"))
            .andDo(
                documentWithResource(
                    "휴면 계정 프로필 조회",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회 - 휴면 계정")
                        .description(
                            """
                            휴면 상태(DORMANT)의 사용자가 프로필을 조회하려고 할 때
                            403 Forbidden을 반환합니다.
                            
                            휴면 계정은 재활성화 절차를 거쳐야 정상적으로 서비스를 이용할 수 있습니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 액세스 토큰} (휴면 계정)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - BANNED 상태 사용자가 자신의 프로필을 조회할 때 403 Forbidden을 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $bannedUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_BANNED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("차단된 계정입니다. 관리자에게 문의해주세요"))
            .andDo(
                documentWithResource(
                    "정지된 계정 프로필 조회",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회 - 정지된 계정")
                        .description(
                            """
                            정지된 상태(BANNED)의 사용자가 프로필을 조회하려고 할 때
                            403 Forbidden을 반환합니다.
                            
                            정지된 계정은 관리자의 제재 해제가 필요합니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 액세스 토큰} (정지된 계정)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - 삭제된 사용자의 토큰으로 요청할 때 410 Gone을 반환해야 한다`() {
        // Given
        val deletedUserToken = jwtTokenManager.generateAccessToken(deletedUser.id).token

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $deletedUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isGone)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_DELETED"))
            .andDo(
                documentWithResource(
                    "삭제된 계정으로 프로필 조회",
                    builder()
                        .tag("User Profile")
                        .summary("내 프로필 조회 - 삭제된 계정")
                        .description(
                            """
                            삭제된 상태(DELETED)의 사용자 토큰으로 프로필을 조회하려고 할 때
                            410 Gone를 반환합니다.
                            
                            삭제된 계정은 복구할 수 없으며, 새로운 계정을 생성해야 합니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 액세스 토큰} (삭제된 계정)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET me - 대소문자 혼용된 Bearer로 요청하면 401 Unauthorized를 반환해야 한다`() {
        // When & Then - "bearer" (소문자)
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "bearer $activeUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        // When & Then - "BEARER" (대문자)
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "BEARER $activeUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        // When & Then - "BeArEr" (혼용)
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "BeArEr $activeUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `GET me - Bearer와 토큰 사이에 여러 공백이 있어도 성공해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer    $activeUserToken") // 여러 공백
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `GET me - Accept 헤더가 없어도 JSON 응답을 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $activeUserToken")
                .contentType(MediaType.APPLICATION_JSON),
            // Accept 헤더 생략
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `GET me - 잘못된 Content-Type이어도 GET 요청은 성공해야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $activeUserToken")
                .contentType(MediaType.TEXT_PLAIN) // 잘못된 Content-Type
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `GET me - 응답의 날짜 형식이 ISO-8601 형식이어야 한다`() {
        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $activeUserToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            // ISO-8601 형식 확인 (예: 2024-01-15T10:30:00Z)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.createdAt")
                    .value(Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")),
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.modifiedAt")
                    .value(Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")),
            )
    }

    @Test
    fun `GET me - 존재하지 않는 사용자 ID의 유효한 토큰으로 요청할 때 404 Not Found를 반환해야 한다`() {
        // Given - 존재하지 않는 사용자 ID로 토큰 생성 (실제로는 불가능한 시나리오지만 테스트)
        val nonExistentUserId = UserId(999999999999L)
        val tokenForNonExistentUser = jwtTokenManager.generateAccessToken(nonExistentUserId).token

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $tokenForNonExistentUser")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_NOT_FOUND"))
    }
}
