package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.TokenType
import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.domain.port.outbound.RefreshTokenStore
import me.helloc.techwikiplus.user.domain.port.outbound.TokenConfiguration
import me.helloc.techwikiplus.user.domain.port.outbound.TokenProvider
import java.time.Duration

class TokenRefresher(
    private val tokenProvider: TokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val tokenConfiguration: TokenConfiguration,
) {
    fun refreshTokens(refreshToken: String): RefreshDetails {
        validateRefreshToken(refreshToken)

        val email = tokenProvider.getEmailFromToken(refreshToken)
        val userId = tokenProvider.getUserIdFromToken(refreshToken)

        val accessToken = tokenProvider.createAccessToken(email, userId)
        val newRefreshToken = tokenProvider.createRefreshToken(email, userId)

        // Store a new refresh token (this will handle the rotation logic internally)
        val ttl = Duration.ofMillis(tokenConfiguration.refreshTokenExpiration)
        refreshTokenStore.store(userId, newRefreshToken, ttl)

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
        if (tokenType != TokenType.REFRESH) {
            throw CustomException.AuthenticationException.InvalidTokenType()
        }

        // Check if a refresh token exists in store
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
