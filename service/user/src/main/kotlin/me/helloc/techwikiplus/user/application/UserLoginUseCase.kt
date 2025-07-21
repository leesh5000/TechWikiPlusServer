package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.TokenProvider
import me.helloc.techwikiplus.user.domain.service.UserAuthenticationService
import me.helloc.techwikiplus.user.domain.service.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
open class UserLoginUseCase(
    private val userReader: UserReader,
    private val userAuthenticationService: UserAuthenticationService,
    private val tokenProvider: TokenProvider
) {

    fun login(email: String, password: String): LoginResult {
        val user = userReader.readByEmailOrThrows(email)
        val authenticatedUser = userAuthenticationService.authenticate(user, password)

        val accessToken = tokenProvider.createAccessToken(authenticatedUser.email(), authenticatedUser.id)
        val refreshToken = tokenProvider.createRefreshToken(authenticatedUser.email(), authenticatedUser.id)

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
