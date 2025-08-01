package me.helloc.techwikiplus.service.user.domain.service.port

interface PasswordEncoder {
    fun encode(rawPassword: String): String

    fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean
}
