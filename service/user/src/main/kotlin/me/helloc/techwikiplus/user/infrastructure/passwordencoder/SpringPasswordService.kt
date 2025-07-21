package me.helloc.techwikiplus.user.infrastructure.passwordencoder

import me.helloc.techwikiplus.user.domain.service.UserPasswordService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class SpringPasswordService(
    val passwordEncoder: PasswordEncoder
) : UserPasswordService {

    override fun encode(password: String): String {
        return passwordEncoder.encode(password)
    }

    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }
}
