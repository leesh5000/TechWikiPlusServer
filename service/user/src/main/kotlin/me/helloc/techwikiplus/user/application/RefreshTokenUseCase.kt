package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.TokenRefresher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
open class RefreshTokenUseCase(
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
