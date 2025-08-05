package me.helloc.techwikiplus.service.user.infrastructure.security

import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordCrypter

// Fake implementation of PasswordEncoder for testing
class FakePasswordCrypter : PasswordCrypter {
    override fun encode(rawPassword: RawPassword): EncodedPassword {
        // For testing purposes, we'll use a simple prefix-based encoding
        return EncodedPassword("FAKE_ENCODED:${rawPassword.value}")
    }

    override fun matches(
        rawPassword: RawPassword,
        encodedPassword: EncodedPassword,
    ): Boolean {
        // Check if the encoded password matches our fake encoding
        return encodedPassword.value == "FAKE_ENCODED:${rawPassword.value}"
    }
}
