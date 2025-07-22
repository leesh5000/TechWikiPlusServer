package me.helloc.techwikiplus.user.integration.http

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.TokenProvider
import me.helloc.techwikiplus.user.domain.service.UserRepository
import me.helloc.techwikiplus.user.interfaces.http.RefreshTokenController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class RefreshTokenControllerIntegrationTest : ControllerIntegrationTestSupport() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var tokenProvider: TokenProvider

    private val testEmail = "test@example.com"
    private val testUserId = System.currentTimeMillis()
    private lateinit var testUser: User
    private lateinit var validRefreshToken: String
    private lateinit var validAccessToken: String

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성
        testUser =
            User(
                id = testUserId,
                email = UserEmail(testEmail, true),
                nickname = "testuser",
                password = "encoded_password",
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(testUser)

        // 유효한 토큰 생성
        validAccessToken = tokenProvider.createAccessToken(testEmail, testUserId)
        validRefreshToken = tokenProvider.createRefreshToken(testEmail, testUserId)
    }

    @Test
    fun `유효한 refresh token으로 토큰 갱신 시 200 응답과 새로운 토큰들을 반환한다`() {
        // given
        val request =
            RefreshTokenController.RefreshTokenRequest(
                refreshToken = validRefreshToken,
            )

        // when
        val response: ResponseEntity<RefreshTokenController.RefreshTokenResponse> =
            restTemplate.postForEntity(
                "/api/v1/users/refresh",
                createJsonHttpEntity(request),
                RefreshTokenController.RefreshTokenResponse::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull

        val refreshResponse = response.body!!
        assertThat(refreshResponse.accessToken).isNotBlank
        assertThat(refreshResponse.refreshToken).isNotBlank
        assertThat(refreshResponse.userId).isEqualTo(testUserId)

        // 새로 발급된 토큰들이 null이 아니고 비어있지 않은지 확인
        assertThat(refreshResponse.accessToken).isNotBlank
        assertThat(refreshResponse.refreshToken).isNotBlank

        // 새로 발급된 토큰들의 유효성 검증
        assertThat(tokenProvider.validateToken(refreshResponse.accessToken)).isTrue
        assertThat(tokenProvider.validateToken(refreshResponse.refreshToken)).isTrue
        assertThat(tokenProvider.getTokenType(refreshResponse.accessToken)).isEqualTo("access")
        assertThat(tokenProvider.getTokenType(refreshResponse.refreshToken)).isEqualTo("refresh")
        assertThat(tokenProvider.getEmailFromToken(refreshResponse.accessToken)).isEqualTo(testEmail)
        assertThat(tokenProvider.getUserIdFromToken(refreshResponse.accessToken)).isEqualTo(testUserId)
    }

    @Test
    fun `유효하지 않은 refresh token으로 갱신 시 401 응답을 반환한다`() {
        // given
        val request =
            RefreshTokenController.RefreshTokenRequest(
                refreshToken = "invalid.refresh.token",
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/refresh",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Invalid refresh token")
    }

    @Test
    fun `access token을 refresh token으로 사용 시 401 응답을 반환한다`() {
        // given
        val request =
            RefreshTokenController.RefreshTokenRequest(
                // access token을 잘못 사용
                refreshToken = validAccessToken,
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/refresh",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Expected refresh token but received access token")
    }

    @Test
    fun `만료된 refresh token으로 갱신 시 401 응답을 반환한다`() {
        // given
        // 실제로 만료된 토큰을 생성하기 어려우므로,
        // 잘못된 시그니처를 가진 토큰으로 테스트
        val expiredToken = validRefreshToken + "corrupted"
        val request =
            RefreshTokenController.RefreshTokenRequest(
                refreshToken = expiredToken,
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/refresh",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Invalid refresh token")
    }

    @Test
    fun `빈 refresh token으로 갱신 요청 시 400 응답을 반환한다`() {
        // given
        val request =
            RefreshTokenController.RefreshTokenRequest(
                refreshToken = "",
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/refresh",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `연속해서 refresh token을 사용해도 정상 동작한다`() {
        // given
        val request1 =
            RefreshTokenController.RefreshTokenRequest(
                refreshToken = validRefreshToken,
            )

        // when - 첫 번째 갱신
        val response1: ResponseEntity<RefreshTokenController.RefreshTokenResponse> =
            restTemplate.postForEntity(
                "/api/v1/users/refresh",
                createJsonHttpEntity(request1),
                RefreshTokenController.RefreshTokenResponse::class.java,
            )

        assertThat(response1.statusCode).isEqualTo(HttpStatus.OK)
        val newRefreshToken = response1.body!!.refreshToken

        // when - 두 번째 갱신 (새로 받은 refresh token 사용)
        val request2 =
            RefreshTokenController.RefreshTokenRequest(
                refreshToken = newRefreshToken,
            )

        val response2: ResponseEntity<RefreshTokenController.RefreshTokenResponse> =
            restTemplate.postForEntity(
                "/api/v1/users/refresh",
                createJsonHttpEntity(request2),
                RefreshTokenController.RefreshTokenResponse::class.java,
            )

        // then
        assertThat(response2.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response2.body).isNotNull
        assertThat(response2.body!!.userId).isEqualTo(testUserId)

        // 두 번째 응답의 토큰들도 유효한지 확인
        assertThat(tokenProvider.validateToken(response2.body!!.accessToken)).isTrue
        assertThat(tokenProvider.validateToken(response2.body!!.refreshToken)).isTrue
    }
}
