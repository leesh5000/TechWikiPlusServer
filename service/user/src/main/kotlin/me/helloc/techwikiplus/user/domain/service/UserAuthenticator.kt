package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidCredentialsException
import me.helloc.techwikiplus.user.domain.port.outbound.PasswordEncoder

class UserAuthenticator(
    private val passwordEncoder: PasswordEncoder,
) {
    fun authenticate(
        user: User,
        password: String,
    ): User {
        if (!passwordEncoder.matches(password, user.password)) {
            throw InvalidCredentialsException()
        }

        user.status.validateForAuthentication()

        return user
    }
}
