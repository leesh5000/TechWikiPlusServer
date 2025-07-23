package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.RefreshTokenStore
import me.helloc.techwikiplus.user.domain.service.TokenConfiguration
import me.helloc.techwikiplus.user.domain.service.TokenProvider
import me.helloc.techwikiplus.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.user.domain.service.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional(readOnly = true)
open class UserLoginUseCase(
    private val userReader: UserReader,
    private val userAuthenticator: UserAuthenticator,
    private val tokenProvider: TokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val tokenConfiguration: TokenConfiguration,
) {
    fun login(
        email: String,
        password: String,
    ): TokenResult {
        val user = userReader.readByEmailOrThrows(email)
        val authenticatedUser = userAuthenticator.authenticate(user, password)

        val accessToken = tokenProvider.createAccessToken(authenticatedUser.getEmailValue(), authenticatedUser.id)
        val refreshToken = tokenProvider.createRefreshToken(authenticatedUser.getEmailValue(), authenticatedUser.id)

        // Store refresh token in Redis
        val ttl = Duration.ofMillis(tokenConfiguration.refreshTokenExpiration)
        refreshTokenStore.store(authenticatedUser.id, refreshToken, ttl)

        return TokenResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = authenticatedUser.id,
        )
    }
}
