package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TokenRefresher(
    private val tokenProvider: TokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val jwtProperties: JwtProperties,
) {
    fun refreshTokens(refreshToken: String): RefreshDetails {
        validateRefreshToken(refreshToken)

        val email = tokenProvider.getEmailFromToken(refreshToken)
        val userId = tokenProvider.getUserIdFromToken(refreshToken)

        val accessToken = tokenProvider.createAccessToken(email, userId)
        val newRefreshToken = tokenProvider.createRefreshToken(email, userId)

        // Store new refresh token and invalidate old one
        val ttl = Duration.ofMillis(jwtProperties.refreshTokenExpiration)
        refreshTokenStore.store(userId, newRefreshToken, ttl)

        // Invalidate the old refresh token
        refreshTokenStore.invalidateToken(refreshToken)

        return RefreshDetails(
            accessToken = accessToken,
            refreshToken = newRefreshToken,
            userId = userId,
        )
    }

    private fun validateRefreshToken(refreshToken: String) {
        val isValid = tokenProvider.validateToken(refreshToken)
        if (!isValid) {
            throw CustomException.AuthenticationException.InvalidToken()
        }

        val tokenType = tokenProvider.getTokenType(refreshToken)
        if (tokenType != "refresh") {
            throw CustomException.AuthenticationException.InvalidTokenType()
        }

        // Check if refresh token exists in store
        if (!refreshTokenStore.exists(refreshToken)) {
            throw CustomException.AuthenticationException.InvalidToken()
        }
    }

    data class RefreshDetails(
        val accessToken: String,
        val refreshToken: String,
        val userId: Long,
    )
}
