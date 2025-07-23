package me.helloc.techwikiplus.user.integration.http

import me.helloc.techwikiplus.user.domain.TokenType
import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.RefreshTokenStore
import me.helloc.techwikiplus.user.domain.service.TokenProvider
import me.helloc.techwikiplus.user.domain.service.UserRepository
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
import me.helloc.techwikiplus.user.infrastructure.security.jwt.TestJwtTokenProvider
import me.helloc.techwikiplus.user.interfaces.http.RefreshTokenController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.Duration

class RefreshTokenControllerIntegrationTest : ControllerIntegrationTestSupport() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var tokenProvider: TokenProvider

    @Autowired
    private lateinit var refreshTokenStore: RefreshTokenStore

    @Autowired
    private lateinit var jwtProperties: JwtProperties

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    private lateinit var testEmail: String
    private var testUserId: Long = 0L
    private lateinit var testUser: User
    private lateinit var validRefreshToken: String
    private lateinit var validAccessToken: String

    @BeforeEach
    fun setUp() {
        // Redis 데이터 초기화 - 테스트 시작 전 깨끗한 상태 보장
        val refreshTokenKeys = redisTemplate.keys("refresh_token:*")
        val userRefreshTokenKeys = redisTemplate.keys("user_refresh_token:*")

        if (refreshTokenKeys.isNotEmpty()) {
            redisTemplate.delete(refreshTokenKeys)
        }
        if (userRefreshTokenKeys.isNotEmpty()) {
            redisTemplate.delete(userRefreshTokenKeys)
        }

        // 각 테스트마다 고유한 ID 생성
        testUserId = System.currentTimeMillis()
        testEmail = "test$testUserId@example.com"

        // 시간 지연을 추가하여 토큰이 다른 시간에 생성되도록 함
        Thread.sleep(10)

        // 테스트용 사용자 생성
        testUser =
            User(
                id = testUserId,
                email = UserEmail(testEmail, true),
                // 닉네임 길이 제한으로 인해 숫자를 줄임
                nickname = "user${testUserId % 100000}",
                password = "encoded_password",
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(testUser)

        // 유효한 토큰 생성
        validAccessToken = tokenProvider.createAccessToken(testEmail, testUserId)
        validRefreshToken = tokenProvider.createRefreshToken(testEmail, testUserId)

        // Refresh token을 Redis에 저장
        val ttl = Duration.ofMillis(jwtProperties.refreshTokenExpiration)
        refreshTokenStore.store(testUserId, validRefreshToken, ttl)
    }

    @AfterEach
    fun tearDown() {
        // Redis 데이터 초기화 - refresh_token:* 및 user_refresh_token:* 패턴의 모든 키 삭제
        val refreshTokenKeys = redisTemplate.keys("refresh_token:*")
        val userRefreshTokenKeys = redisTemplate.keys("user_refresh_token:*")

        if (refreshTokenKeys.isNotEmpty()) {
            redisTemplate.delete(refreshTokenKeys)
        }
        if (userRefreshTokenKeys.isNotEmpty()) {
            redisTemplate.delete(userRefreshTokenKeys)
        }
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
        assertThat(tokenProvider.getTokenType(refreshResponse.accessToken)).isEqualTo(TokenType.ACCESS)
        assertThat(tokenProvider.getTokenType(refreshResponse.refreshToken)).isEqualTo(TokenType.REFRESH)
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
        val testTokenProvider = TestJwtTokenProvider(jwtProperties)
        val expiredRefreshToken = testTokenProvider.createExpiredRefreshToken(testEmail, testUserId)
        // 만료된 토큰도 Redis에 저장 (실제 시나리오를 시뮬레이션)
        val ttl = Duration.ofMillis(jwtProperties.refreshTokenExpiration)
        refreshTokenStore.store(testUserId, expiredRefreshToken, ttl)

        val request =
            RefreshTokenController.RefreshTokenRequest(
                refreshToken = expiredRefreshToken,
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
        assertThat(response1.body).isNotNull
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

    @Test
    fun `refresh token rotation으로 이전 토큰은 사용할 수 없다`() {
        // given - 테스트용 고유 사용자 생성
        val uniqueUserId = System.nanoTime() // 나노초를 사용하여 더 고유한 ID 생성
        val uniqueEmail = "rotation-test-$uniqueUserId@example.com"
        val uniqueUser =
            User(
                id = uniqueUserId,
                email = UserEmail(uniqueEmail, true),
                // 닉네임 길이 제한
                nickname = "rot${uniqueUserId % 100000}",
                password = "encoded_password",
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(uniqueUser)

        // 고유한 refresh token 생성
        val uniqueRefreshToken = tokenProvider.createRefreshToken(uniqueEmail, uniqueUserId)
        val ttl = Duration.ofMillis(jwtProperties.refreshTokenExpiration)
        refreshTokenStore.store(uniqueUserId, uniqueRefreshToken, ttl)

        val request1 =
            RefreshTokenController.RefreshTokenRequest(
                refreshToken = uniqueRefreshToken,
            )

        // 초기 상태 확인
        assertThat(refreshTokenStore.exists(uniqueRefreshToken)).isTrue

        // when - 첫 번째 갱신
        val response1: ResponseEntity<RefreshTokenController.RefreshTokenResponse> =
            restTemplate.postForEntity(
                "/api/v1/users/refresh",
                createJsonHttpEntity(request1),
                RefreshTokenController.RefreshTokenResponse::class.java,
            )

        assertThat(response1.statusCode).isEqualTo(HttpStatus.OK)
        val newRefreshToken = response1.body!!.refreshToken

        // 이전 토큰이 무효화되었는지 확인
        assertThat(refreshTokenStore.exists(uniqueRefreshToken))
            .withFailMessage("Old token should not exist after refresh")
            .isFalse

        // 새 토큰이 존재하는지 확인
        assertThat(refreshTokenStore.exists(newRefreshToken))
            .withFailMessage("New token should exist after refresh")
            .isTrue

        // when - 이전 refresh token을 다시 사용
        val response2: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/refresh",
                createJsonHttpEntity(request1),
                String::class.java,
            )

        // then
        assertThat(response2.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        val errorResponse = parseErrorResponse(response2.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Invalid refresh token")
    }
}
