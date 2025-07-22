package me.helloc.techwikiplus.user.infrastructure.passwordencoder.fake

import me.helloc.techwikiplus.user.domain.service.PasswordEncoder

class FakePasswordEncoder : PasswordEncoder {
    override fun encode(password: String): String {
        // Simple encoding for testing - just add a prefix
        return "encoded_$password"
    }

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean {
        return encodedPassword == "encoded_$rawPassword"
    }
}
