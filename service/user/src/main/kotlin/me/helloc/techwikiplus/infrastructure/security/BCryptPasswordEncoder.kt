package me.helloc.techwikiplus.infrastructure.security

import me.helloc.techwikiplus.domain.service.port.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder as SpringBCryptPasswordEncoder

@Component
class BCryptPasswordEncoder : PasswordEncoder {
    private val encoder = SpringBCryptPasswordEncoder()

    override fun encode(rawPassword: String): String {
        // BCrypt has a 72-byte limit
        val passwordToEncode =
            if (rawPassword.toByteArray().size > BCRYPT_MAX_BYTES) {
                String(rawPassword.toByteArray().take(BCRYPT_MAX_BYTES).toByteArray())
            } else {
                rawPassword
            }
        return encoder.encode(passwordToEncode)
    }

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean {
        // Apply the same truncation logic for matching
        val passwordToMatch =
            if (rawPassword.toByteArray().size > BCRYPT_MAX_BYTES) {
                String(rawPassword.toByteArray().take(BCRYPT_MAX_BYTES).toByteArray())
            } else {
                rawPassword
            }
        return encoder.matches(passwordToMatch, encodedPassword)
    }

    companion object {
        private const val BCRYPT_MAX_BYTES = 72
    }
}
