package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.BannedUserException
import me.helloc.techwikiplus.service.user.domain.exception.DormantUserException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.exception.PendingUserException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordCrypter

class UserAuthenticator(
    private val crypter: PasswordCrypter,
) {
    fun authenticateOrThrows(
        user: User,
        rawPassword: RawPassword,
    ) {
        validateOrThrows(user)
        if (!crypter.matches(rawPassword, user.encodedPassword)) {
            throw InvalidCredentialsException()
        }
    }

    fun validateOrThrows(user: User) {
        when (user.status) {
            UserStatus.ACTIVE -> return
            UserStatus.PENDING -> throw PendingUserException()
            UserStatus.DORMANT -> throw DormantUserException()
            UserStatus.BANNED -> throw BannedUserException()
            UserStatus.DELETED -> throw UserNotFoundException("User has been deleted")
        }
    }
}
