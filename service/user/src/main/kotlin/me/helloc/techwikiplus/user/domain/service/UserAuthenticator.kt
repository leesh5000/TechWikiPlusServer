package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
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

        user.status.validateForAuthentication()

        return user
    }
}
