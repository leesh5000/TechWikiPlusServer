package me.helloc.techwikiplus.service.user.application.service

import jakarta.transaction.Transactional
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserTokenService
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserLoginUseCase
import org.springframework.stereotype.Component

@Transactional
@Component
class UserLoginFacade(
    private val reader: UserReader,
    private val authenticator: UserAuthenticator,
    private val userTokenService: UserTokenService,
) : UserLoginUseCase {
    override fun execute(
        email: Email,
        password: RawPassword,
    ): UserLoginUseCase.Result {
        // Retrieve user by email
        val user = reader.get(email)

        // Authenticate user
        authenticator.authenticate(user, password)

        // Generate tokens
        val tokenPair = userTokenService.generateTokens(user.id)

        // Return result
        return UserLoginUseCase.Result(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            userId = user.id,
        )
    }
}
