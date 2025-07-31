package me.helloc.techwikiplus.domain.service.port

interface PasswordEncoder {
    fun encode(rawPassword: String): String

    fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean
}
