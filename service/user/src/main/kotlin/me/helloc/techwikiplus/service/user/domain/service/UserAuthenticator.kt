package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotActiveException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository

class UserAuthenticator(
    private val userRepository: UserRepository,
    private val passwordService: UserPasswordService,
) {
    fun authenticate(
        email: Email,
        rawPassword: RawPassword,
    ): User {
        val user =
            userRepository.findBy(email)
                ?: throw UserNotFoundException(email.value)

        if (!passwordService.matches(rawPassword, user.encodedPassword)) {
            throw InvalidCredentialsException()
        }

        return when (user.status) {
            UserStatus.ACTIVE -> user
            UserStatus.DORMANT -> throw UserNotActiveException("User account is dormant")
            UserStatus.BANNED -> throw UserNotActiveException("User account is banned")
            UserStatus.PENDING -> throw UserNotActiveException("User account is pending activation")
            UserStatus.DELETED -> throw UserNotActiveException("User account is deleted")
        }
    }
}
