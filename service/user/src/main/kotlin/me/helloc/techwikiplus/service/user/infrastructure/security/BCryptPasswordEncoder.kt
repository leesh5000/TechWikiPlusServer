package me.helloc.techwikiplus.service.user.infrastructure.security

import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncoder
import org.springframework.security.crypto.factory.PasswordEncoderFactories

class BCryptPasswordEncoder : PasswordEncoder {
    private val encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

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
