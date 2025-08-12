package me.helloc.techwikiplus.service.user.application.facade

import jakarta.transaction.Transactional
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserTokenService
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserLoginRefreshUseCase
import org.springframework.stereotype.Component

@Transactional
@Component
class UserLoginRefreshFacade(
    private val reader: UserReader,
    private val authenticator: UserAuthenticator,
    private val userTokenService: UserTokenService,
) : UserLoginRefreshUseCase {
    override fun execute(
        userId: UserId,
        refreshToken: String,
    ): UserLoginRefreshUseCase.Result {
        val activeUser = reader.get(userId)
        authenticator.authenticate(
            user = activeUser,
            refreshToken = refreshToken,
        )

        // Generate new tokens
        val tokenPair = userTokenService.generateTokens(activeUser.id)

        // Return result with new tokens
        return UserLoginRefreshUseCase.Result(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            userId = activeUser.id,
        )
    }
}
