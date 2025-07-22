package me.helloc.techwikiplus.user.infrastructure.passwordencoder

import me.helloc.techwikiplus.user.domain.service.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.security.crypto.password.PasswordEncoder as SpringPasswordEncoderInterface

@Component
class SpringPasswordEncoder(
    private val passwordEncoder: SpringPasswordEncoderInterface,
) : PasswordEncoder {
    override fun encode(password: String): String {
        return passwordEncoder.encode(password)
    }

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }
}
