package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.TokenType
import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.TokenRefresher
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.infrastructure.passwordencoder.fake.FakePasswordEncoder
import me.helloc.techwikiplus.user.infrastructure.persistence.fake.FakeUserRepository
import me.helloc.techwikiplus.user.infrastructure.refreshtoken.fake.FakeRefreshTokenStore
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
import me.helloc.techwikiplus.user.infrastructure.security.fake.FakeTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class RefreshTokenUseCaseUnitTest {
    private lateinit var userRepository: FakeUserRepository
    private lateinit var userReader: UserReader
    private lateinit var tokenProvider: FakeTokenProvider
    private lateinit var tokenRefresher: TokenRefresher
    private lateinit var refreshTokenStore: FakeRefreshTokenStore
    private lateinit var jwtProperties: JwtProperties
    private lateinit var refreshTokenUseCase: RefreshTokenUseCase

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userReader = UserReader(userRepository)
        tokenProvider = FakeTokenProvider()
        refreshTokenStore = FakeRefreshTokenStore()
        jwtProperties =
            JwtProperties().apply {
                refreshTokenExpiration = 604800000 // 7 days
            }
        tokenRefresher = TokenRefresher(tokenProvider, refreshTokenStore, jwtProperties)
        refreshTokenUseCase =
            RefreshTokenUseCase(
                tokenRefresher = tokenRefresher,
            )
    }

    @Test
    fun shouldCreateNewTokensWithValidRefreshToken() {
        // given
        val email = "test@example.com"
        val userId = 1L
        val passwordEncoder = FakePasswordEncoder()
        val encodedPassword = passwordEncoder.encode("password123")

        val user =
            User(
                id = userId,
                email = UserEmail(email, true),
                nickname = "testuser",
                password = encodedPassword,
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(user)

        val refreshToken = tokenProvider.createRefreshToken(email, userId)
        // Refresh token을 store에 저장
        refreshTokenStore.store(userId, refreshToken, Duration.ofDays(7))

        // when
        val result = refreshTokenUseCase.refresh(refreshToken)

        // then
        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.accessToken).isNotBlank()
        assertThat(result.refreshToken).isNotBlank()
        assertThat(result.accessToken).isNotEqualTo(refreshToken)
        assertThat(result.refreshToken).isNotEqualTo(refreshToken)

        // 새로 발급된 토큰들이 올바른 정보를 담고 있는지 확인
        assertThat(tokenProvider.getEmailFromToken(result.accessToken)).isEqualTo(email)
        assertThat(tokenProvider.getUserIdFromToken(result.accessToken)).isEqualTo(userId)
        assertThat(tokenProvider.getTokenType(result.accessToken)).isEqualTo(TokenType.ACCESS)

        assertThat(tokenProvider.getEmailFromToken(result.refreshToken)).isEqualTo(email)
        assertThat(tokenProvider.getUserIdFromToken(result.refreshToken)).isEqualTo(userId)
        assertThat(tokenProvider.getTokenType(result.refreshToken)).isEqualTo(TokenType.REFRESH)
    }

    @Test
    fun shouldThrowExceptionWhenRefreshTokenIsInvalid() {
        // given
        val invalidToken = "invalid.refresh.token"

        // when & then
        assertThatThrownBy {
            refreshTokenUseCase.refresh(invalidToken)
        }.isInstanceOf(CustomException.AuthenticationException.InvalidToken::class.java)
            .hasMessage("Invalid refresh token")
    }

    @Test
    fun shouldThrowExceptionWhenAccessTokenIsUsedAsRefreshToken() {
        // given
        val email = "test@example.com"
        val userId = 1L
        val passwordEncoder = FakePasswordEncoder()
        val encodedPassword = passwordEncoder.encode("password123")

        val user =
            User(
                id = userId,
                email = UserEmail(email, true),
                nickname = "testuser",
                password = encodedPassword,
                status = UserStatus.ACTIVE,
                createdAt = Clock.system.localDateTime(),
                updatedAt = Clock.system.localDateTime(),
            )
        userRepository.insertOrUpdate(user)

        // Access token을 생성
        val accessToken = tokenProvider.createAccessToken(email, userId)
        // Access token을 store에 저장 (잘못된 사용)
        refreshTokenStore.store(userId, accessToken, Duration.ofDays(7))

        // when & then
        assertThatThrownBy {
            refreshTokenUseCase.refresh(accessToken)
        }.isInstanceOf(CustomException.AuthenticationException.InvalidTokenType::class.java)
            .hasMessage("Expected refresh token but received access token")
    }
}
