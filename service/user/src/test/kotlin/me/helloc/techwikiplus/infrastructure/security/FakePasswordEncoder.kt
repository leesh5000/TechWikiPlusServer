package me.helloc.techwikiplus.infrastructure.security

import me.helloc.techwikiplus.domain.service.port.PasswordEncoder

// Fake implementation of PasswordEncoder for testing
class FakePasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: String): String {
        // For testing purposes, we'll use a simple prefix-based encoding
        return "FAKE_ENCODED:$rawPassword"
    }

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean {
        // Check if the encoded password matches our fake encoding
        return encodedPassword == encode(rawPassword)
    }
}
