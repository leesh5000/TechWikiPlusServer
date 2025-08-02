package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncoder

class UserPasswordService(
    private val passwordEncoder: PasswordEncoder,
) {
    fun encode(rawPassword: String): EncodedPassword {
        EncodedPassword.validateRawPasswordPolicy(rawPassword)
        return EncodedPassword(passwordEncoder.encode(rawPassword))
    }

    fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }
}
