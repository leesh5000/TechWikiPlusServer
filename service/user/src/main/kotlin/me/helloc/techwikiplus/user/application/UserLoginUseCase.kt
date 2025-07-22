package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.RefreshTokenStore
import me.helloc.techwikiplus.user.domain.service.TokenProvider
import me.helloc.techwikiplus.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
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
    private val jwtProperties: JwtProperties,
) {
    fun login(
        email: String,
        password: String,
    ): LoginResult {
        val user = userReader.readByEmailOrThrows(email)
        val authenticatedUser = userAuthenticator.authenticate(user, password)

        val accessToken = tokenProvider.createAccessToken(authenticatedUser.email(), authenticatedUser.id)
        val refreshToken = tokenProvider.createRefreshToken(authenticatedUser.email(), authenticatedUser.id)

        // Store refresh token in Redis
        val ttl = Duration.ofMillis(jwtProperties.refreshTokenExpiration)
        refreshTokenStore.store(authenticatedUser.id, refreshToken, ttl)

        return LoginResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = authenticatedUser.id,
        )
    }

    data class LoginResult(
        val accessToken: String,
        val refreshToken: String,
        val userId: Long,
    )
}
