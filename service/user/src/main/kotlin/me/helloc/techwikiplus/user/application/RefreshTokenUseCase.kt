package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.TokenRefresher
import org.slf4j.LoggerFactory

class RefreshTokenUseCase(
    private val tokenRefresher: TokenRefresher,
    private val exceptionHandler: ApplicationExceptionHandler =
        ApplicationExceptionHandler(
            LoggerFactory.getLogger(RefreshTokenUseCase::class.java),
        ),
) {
    /**
     * 리프레시 토큰을 사용하여 새로운 토큰을 발급한다
     *
     * @throws IllegalArgumentException 유효하지 않은 리프레시 토큰인 경우
     * @throws InfrastructureException 토큰 저장 실패 시
     */
    fun refresh(refreshToken: String): TokenResult =
        exceptionHandler.execute("RefreshToken") {
            val result = tokenRefresher.refreshTokens(refreshToken)

            TokenResult(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                userId = result.userId,
            )
        }
}
