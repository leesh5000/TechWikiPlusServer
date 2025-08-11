package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
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
            throw DomainException(ErrorCode.INVALID_CREDENTIALS)
        }
    }

    @Throws(DomainException::class)
    fun authenticate(
        user: User,
        refreshToken: String,
    ): UserId {
        user.validateUserStatus()
        return tokenManager.validateRefreshToken(user.id, refreshToken)
    }
}
