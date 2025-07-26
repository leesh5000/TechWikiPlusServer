package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.TokenRefresher

class RefreshTokenUseCase(
    private val tokenRefresher: TokenRefresher,
) {
    fun refresh(refreshToken: String): TokenResult {
        val result = tokenRefresher.refreshTokens(refreshToken)

        return TokenResult(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            userId = result.userId,
        )
    }
}
