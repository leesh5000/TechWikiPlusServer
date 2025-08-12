package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncryptor
import me.helloc.techwikiplus.service.user.domain.service.port.TokenManager
import org.springframework.stereotype.Service

@Service
class UserAuthenticator(
    private val encryptor: PasswordEncryptor,
    private val tokenManager: TokenManager,
) {
    fun authenticate(
        user: User,
        rawPassword: RawPassword,
    ) {
        user.validateUserStatus()
        if (!encryptor.matches(rawPassword, user.encodedPassword)) {
            throw UserDomainException(UserErrorCode.INVALID_CREDENTIALS)
        }
    }

    @Throws(UserDomainException::class)
    fun authenticate(
        user: User,
        refreshToken: String,
    ): UserId {
        user.validateUserStatus()
        return tokenManager.validateRefreshToken(user.id, refreshToken)
    }
}
