package me.helloc.techwikiplus.service.user.application

import jakarta.transaction.Transactional
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.TokenService
import me.helloc.techwikiplus.service.user.domain.service.UserPasswordService
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserLoginUseCase
import org.springframework.stereotype.Component

@Transactional
@Component
class UserLoginFacade(
    private val reader: UserReader,
    private val userPasswordService: UserPasswordService,
    private val tokenService: TokenService,
) : UserLoginUseCase {
    override fun execute(command: UserLoginUseCase.Command): UserLoginUseCase.Result {
        // Convert string inputs to value objects
        val email = Email(command.email)
        val rawPassword = RawPassword(command.password)

        // Get Active User
        val user = reader.getActiveUserBy(email)

        // Authenticate user
        userPasswordService.matchOrThrows(rawPassword, user.encodedPassword)

        // Generate tokens
        val tokenPair = tokenService.generateTokens(user.id)

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
