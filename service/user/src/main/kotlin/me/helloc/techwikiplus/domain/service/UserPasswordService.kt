package me.helloc.techwikiplus.domain.service

import me.helloc.techwikiplus.domain.service.port.PasswordEncoder

class UserPasswordService(
    private val passwordEncoder: PasswordEncoder
) {

    fun encode(rawPassword: String): String {
        return passwordEncoder.encode(rawPassword)
    }

    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }
}
