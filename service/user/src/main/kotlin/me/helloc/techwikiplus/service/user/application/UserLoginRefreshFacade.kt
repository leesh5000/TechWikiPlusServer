package me.helloc.techwikiplus.service.user.application

import jakarta.transaction.Transactional
import me.helloc.techwikiplus.service.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserTokenGenerator
import me.helloc.techwikiplus.service.user.domain.service.UserTokenValidator
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserLoginRefreshUseCase
import org.springframework.stereotype.Component

@Transactional
@Component
class UserLoginRefreshFacade(
    private val reader: UserReader,
    private val authenticator: UserAuthenticator,
    private val userTokenGenerator: UserTokenGenerator,
    private val userTokenValidator: UserTokenValidator,
) : UserLoginRefreshUseCase {
    override fun execute(command: UserLoginRefreshUseCase.Command): UserLoginRefreshUseCase.Result {
        // Validate refresh token and extract claims
        val claims = userTokenValidator.validateRefreshTokenOrThrows(command.refreshToken)

        // Get user by ID from claims
        val user = reader.getBy(claims.userId)

        // Check if the user is active (throws appropriate exceptions for other statuses)
        authenticator.validateOrThrows(user)

        // Generate new tokens
        val tokenPair = userTokenGenerator.generateTokens(user.id)

        // Return result with new tokens
        return UserLoginRefreshUseCase.Result(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            userId = user.id,
            accessTokenExpiresAt = tokenPair.accessTokenExpiresAt,
            refreshTokenExpiresAt = tokenPair.refreshTokenExpiresAt,
        )
    }
}
