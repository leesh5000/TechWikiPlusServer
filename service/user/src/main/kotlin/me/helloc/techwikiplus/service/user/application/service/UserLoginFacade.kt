package me.helloc.techwikiplus.service.user.application.service

import jakarta.transaction.Transactional
import me.helloc.techwikiplus.service.user.application.port.inbound.UserLoginUseCase
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserTokenGenerator
import org.springframework.stereotype.Component

@Transactional
@Component
class UserLoginFacade(
    private val reader: UserReader,
    private val authenticator: UserAuthenticator,
    private val userTokenGenerator: UserTokenGenerator,
) : UserLoginUseCase {
    override fun execute(command: UserLoginUseCase.Command): UserLoginUseCase.Result {
        // Convert string inputs to value objects
        val email = Email(command.email)
        val rawPassword = RawPassword(command.password)

        // Retrieve user by email
        val user = reader.getBy(email)

        // Authenticate user
        authenticator.authenticateOrThrows(user, rawPassword)

        // Generate tokens
        val tokenPair = userTokenGenerator.generateTokens(user.id)

        // Return result
        return UserLoginUseCase.Result(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            userId = user.id,
            accessTokenExpiresAt = tokenPair.accessTokenExpiresAt,
            refreshTokenExpiresAt = tokenPair.refreshTokenExpiresAt,
        )
    }
}
