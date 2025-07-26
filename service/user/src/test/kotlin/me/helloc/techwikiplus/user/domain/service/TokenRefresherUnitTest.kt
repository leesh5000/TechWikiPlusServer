package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidTokenException
import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidTokenTypeException
import me.helloc.techwikiplus.user.infrastructure.refreshtoken.fake.FakeRefreshTokenStore
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
import me.helloc.techwikiplus.user.infrastructure.security.fake.FakeTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class TokenRefresherUnitTest {
    private lateinit var tokenRefresher: TokenRefresher
    private lateinit var tokenProvider: FakeTokenProvider
    private lateinit var refreshTokenStore: FakeRefreshTokenStore
    private lateinit var jwtProperties: JwtProperties

    @BeforeEach
    fun setUp() {
        tokenProvider = FakeTokenProvider()
        refreshTokenStore = FakeRefreshTokenStore()
        jwtProperties =
            JwtProperties().apply {
                refreshTokenExpiration = 604800000 // 7 days
            }
        tokenRefresher = TokenRefresher(tokenProvider, refreshTokenStore, jwtProperties)
    }

    @Test
    fun `유효한 refresh token으로 새로운 토큰을 발급받는다`() {
        // given
        val email = "test@example.com"
        val userId = 123L
        val refreshToken = tokenProvider.createRefreshToken(email, userId)
        refreshTokenStore.store(userId, refreshToken, Duration.ofDays(7))

        // when
        val result = tokenRefresher.refreshTokens(refreshToken)

        // then
        assertThat(result.accessToken).isNotBlank
        assertThat(result.refreshToken).isNotBlank
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.refreshToken).isNotEqualTo(refreshToken) // 새로운 토큰이어야 함
    }

    @Test
    fun `refresh token 갱신 후 기존 토큰은 무효화된다`() {
        // given
        val email = "test@example.com"
        val userId = 123L
        val oldRefreshToken = tokenProvider.createRefreshToken(email, userId)
        refreshTokenStore.store(userId, oldRefreshToken, Duration.ofDays(7))

        // when
        tokenRefresher.refreshTokens(oldRefreshToken)

        // then
        assertThat(refreshTokenStore.exists(oldRefreshToken)).isFalse
    }

    @Test
    fun `refresh token 갱신 후 새로운 토큰은 저장된다`() {
        // given
        val email = "test@example.com"
        val userId = 123L
        val oldRefreshToken = tokenProvider.createRefreshToken(email, userId)
        refreshTokenStore.store(userId, oldRefreshToken, Duration.ofDays(7))

        // when
        val result = tokenRefresher.refreshTokens(oldRefreshToken)

        // then
        assertThat(refreshTokenStore.exists(result.refreshToken)).isTrue
    }

    @Test
    fun `유효하지 않은 refresh token으로 갱신 시도 시 예외가 발생한다`() {
        // given
        val invalidToken = "invalid.refresh.token"

        // when & then
        assertThatThrownBy { tokenRefresher.refreshTokens(invalidToken) }
            .isInstanceOf(InvalidTokenException::class.java)
    }

    @Test
    fun `access token을 refresh token으로 사용 시 예외가 발생한다`() {
        // given
        val email = "test@example.com"
        val userId = 123L
        val accessToken = tokenProvider.createAccessToken(email, userId)
        refreshTokenStore.store(userId, accessToken, Duration.ofDays(7))

        // when & then
        assertThatThrownBy { tokenRefresher.refreshTokens(accessToken) }
            .isInstanceOf(InvalidTokenTypeException::class.java)
    }

    @Test
    fun `Redis에 저장되지 않은 refresh token 사용 시 예외가 발생한다`() {
        // given
        val email = "test@example.com"
        val userId = 123L
        val refreshToken = tokenProvider.createRefreshToken(email, userId)
        // 토큰을 Redis에 저장하지 않음

        // when & then
        assertThatThrownBy { tokenRefresher.refreshTokens(refreshToken) }
            .isInstanceOf(InvalidTokenException::class.java)
    }

    @Test
    fun `만료된 refresh token 사용 시 예외가 발생한다`() {
        // given
        val email = "test@example.com"
        val userId = 123L
        val expiredToken = tokenProvider.createExpiredRefreshToken(email, userId)
        refreshTokenStore.store(userId, expiredToken, Duration.ofDays(7))

        // when & then
        assertThatThrownBy { tokenRefresher.refreshTokens(expiredToken) }
            .isInstanceOf(InvalidTokenException::class.java)
    }
}
