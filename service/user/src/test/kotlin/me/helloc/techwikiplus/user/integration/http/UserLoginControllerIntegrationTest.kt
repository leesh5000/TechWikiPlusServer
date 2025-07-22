package me.helloc.techwikiplus.user.integration.http

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.PasswordEncoder
import me.helloc.techwikiplus.user.domain.service.RefreshTokenStore
import me.helloc.techwikiplus.user.domain.service.TokenProvider
import me.helloc.techwikiplus.user.domain.service.UserRepository
import me.helloc.techwikiplus.user.interfaces.http.UserLoginController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class UserLoginControllerIntegrationTest : ControllerIntegrationTestSupport() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var tokenProvider: TokenProvider

    @Autowired
    private lateinit var refreshTokenStore: RefreshTokenStore

    private val testEmail = "test@example.com"
    private val testPassword = "ValidPass123!"
    private val testNickname = "testuser"

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성
        val encodedPassword = passwordEncoder.encode(testPassword)
        val user =
            User(
                id = System.currentTimeMillis(),
                email = UserEmail(testEmail, true),
                nickname = testNickname,
                password = encodedPassword,
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(user)
    }

    @Test
    fun `올바른 인증 정보로 로그인 시 200 응답과 JWT 토큰을 반환한다`() {
        // given
        val request =
            UserLoginController.LoginRequest(
                email = testEmail,
                password = testPassword,
            )

        // when
        val response: ResponseEntity<UserLoginController.LoginResponse> =
            restTemplate.postForEntity(
                "/api/v1/users/login",
                createJsonHttpEntity(request),
                UserLoginController.LoginResponse::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull

        val loginResponse = response.body!!
        assertThat(loginResponse.accessToken).isNotBlank
        assertThat(loginResponse.refreshToken).isNotBlank
        assertThat(loginResponse.userId).isGreaterThan(0)

        // 발급된 토큰 검증
        assertThat(tokenProvider.validateToken(loginResponse.accessToken)).isTrue
        assertThat(tokenProvider.validateToken(loginResponse.refreshToken)).isTrue
        assertThat(tokenProvider.getTokenType(loginResponse.accessToken)).isEqualTo("access")
        assertThat(tokenProvider.getTokenType(loginResponse.refreshToken)).isEqualTo("refresh")
        assertThat(tokenProvider.getEmailFromToken(loginResponse.accessToken)).isEqualTo(testEmail)

        // Refresh token이 Redis에 저장되었는지 확인
        assertThat(refreshTokenStore.exists(loginResponse.refreshToken)).isTrue
    }

    @Test
    fun `잘못된 비밀번호로 로그인 시 401 응답을 반환한다`() {
        // given
        val request =
            UserLoginController.LoginRequest(
                email = testEmail,
                password = "WrongPassword123!",
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/login",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Invalid email or password")
    }

    @Test
    fun `존재하지 않는 이메일로 로그인 시 401 응답을 반환한다`() {
        // given
        val request =
            UserLoginController.LoginRequest(
                email = "nonexistent@example.com",
                password = testPassword,
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/login",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("NOT_FOUND")
        assertThat(errorResponse.message).contains("User not found")
    }

    @Test
    fun `이메일 미인증 사용자로 로그인 시 401 응답을 반환한다`() {
        // given
        val unverifiedEmail = "unverified@example.com"
        val encodedPassword = passwordEncoder.encode(testPassword)
        val unverifiedUser =
            User(
                id = System.currentTimeMillis() + 1,
                // 미인증 이메일
                email = UserEmail(unverifiedEmail, false),
                nickname = "unverifieduser",
                password = encodedPassword,
                status = UserStatus.PENDING,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(unverifiedUser)

        val request =
            UserLoginController.LoginRequest(
                email = unverifiedEmail,
                password = testPassword,
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/login",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Email not verified")
    }

    @Test
    fun `정지된 사용자로 로그인 시 401 응답을 반환한다`() {
        // given
        val bannedEmail = "banned@example.com"
        val encodedPassword = passwordEncoder.encode(testPassword)
        val bannedUser =
            User(
                id = System.currentTimeMillis() + 2,
                email = UserEmail(bannedEmail, true),
                nickname = "banneduser",
                password = encodedPassword,
                status = UserStatus.BANNED,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(bannedUser)

        val request =
            UserLoginController.LoginRequest(
                email = bannedEmail,
                password = testPassword,
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/login",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Your account has been banned")
    }
}
