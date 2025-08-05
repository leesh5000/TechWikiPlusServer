package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.application.port.outbound.ClockHolder
import me.helloc.techwikiplus.service.user.application.port.outbound.TokenGenerator
import java.time.Instant

class UserTokenGenerator(
    private val tokenGenerator: TokenGenerator,
    private val clockHolder: ClockHolder,
) {
    fun generateTokens(userId: String): TokenPair {
        val now = clockHolder.now()
        val accessToken = tokenGenerator.generateAccessToken(userId)
        val refreshToken = tokenGenerator.generateRefreshToken(userId)

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresAt = now.plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS),
            refreshTokenExpiresAt = now.plusSeconds(REFRESH_TOKEN_VALIDITY_SECONDS),
        )
    }

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val accessTokenExpiresAt: Instant,
        val refreshTokenExpiresAt: Instant,
    )

    companion object {
        private const val ACCESS_TOKEN_VALIDITY_SECONDS = 3600L // 1 hour
        private const val REFRESH_TOKEN_VALIDITY_SECONDS = 2592000L // 30 days
    }
}
