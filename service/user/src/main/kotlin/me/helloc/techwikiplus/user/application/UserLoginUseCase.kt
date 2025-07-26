package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.port.outbound.RefreshTokenStore
import me.helloc.techwikiplus.user.domain.port.outbound.TokenConfiguration
import me.helloc.techwikiplus.user.domain.port.outbound.TokenProvider
import me.helloc.techwikiplus.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.user.domain.service.UserReader
import org.slf4j.LoggerFactory
import java.time.Duration

class UserLoginUseCase(
    private val userReader: UserReader,
    private val userAuthenticator: UserAuthenticator,
    private val tokenProvider: TokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val tokenConfiguration: TokenConfiguration,
    private val exceptionHandler: ApplicationExceptionHandler =
        ApplicationExceptionHandler(
            LoggerFactory.getLogger(UserLoginUseCase::class.java),
        ),
) {
    /**
     * 사용자 로그인을 처리한다
     * 인증 성공 시 액세스 토큰과 리프레시 토큰을 발급한다
     *
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     * @throws IllegalStateException 비밀번호가 일치하지 않는 경우
     * @throws InfrastructureException 토큰 저장 실패 시
     */
    fun login(
        email: String,
        password: String,
    ): TokenResult =
        exceptionHandler.execute("UserLogin") {
            val user = userReader.readByEmailOrThrows(email)
            val authenticatedUser = userAuthenticator.authenticate(user, password)

            val accessToken = tokenProvider.createAccessToken(authenticatedUser.getEmailValue(), authenticatedUser.id)
            val refreshToken = tokenProvider.createRefreshToken(authenticatedUser.getEmailValue(), authenticatedUser.id)

            // Store refresh token in Redis
            val ttl = Duration.ofMillis(tokenConfiguration.refreshTokenExpiration)
            refreshTokenStore.store(authenticatedUser.id, refreshToken, ttl)

            TokenResult(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = authenticatedUser.id,
            )
        }
}
