package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.springframework.stereotype.Component

@Component
class TokenRefresher(
    private val tokenProvider: TokenProvider,
) {
    fun refreshTokens(refreshToken: String): RefreshDetails {
        validateRefreshToken(refreshToken)

        val email = tokenProvider.getEmailFromToken(refreshToken)
        val userId = tokenProvider.getUserIdFromToken(refreshToken)

        val accessToken = tokenProvider.createAccessToken(email, userId)
        val newRefreshToken = tokenProvider.createRefreshToken(email, userId)

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
    }

    data class RefreshDetails(
        val accessToken: String,
        val refreshToken: String,
        val userId: Long,
    )
}
