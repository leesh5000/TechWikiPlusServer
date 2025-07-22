package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.springframework.stereotype.Service

@Service
class UserAuthenticator(
    private val passwordEncoder: PasswordEncoder,
) {
    fun authenticate(
        user: User,
        password: String,
    ): User {
        if (!passwordEncoder.matches(password, user.password)) {
            throw CustomException.AuthenticationException.InvalidCredentials()
        }

        validateUserStatus(user)

        return user
    }

    private fun validateUserStatus(user: User) {
        if (user.status != UserStatus.ACTIVE) {
            when (user.status) {
                UserStatus.PENDING -> throw CustomException.AuthenticationException.EmailNotVerified()
                UserStatus.BANNED -> throw CustomException.AuthenticationException.AccountBanned()
                UserStatus.DORMANT -> throw CustomException.AuthenticationException.AccountDormant()
                UserStatus.DELETED -> throw CustomException.AuthenticationException.AccountDeleted()
                else -> throw CustomException.AuthenticationException.InvalidCredentials()
            }
        }
    }
}
