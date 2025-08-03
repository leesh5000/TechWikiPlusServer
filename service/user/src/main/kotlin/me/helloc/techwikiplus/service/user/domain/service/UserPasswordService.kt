package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncoder

class UserPasswordService(
    private val passwordEncoder: PasswordEncoder,
) {
    fun encode(rawPassword: RawPassword): EncodedPassword {
        return passwordEncoder.encode(rawPassword)
    }

    fun matches(
        rawPassword: RawPassword,
        encodedPassword: EncodedPassword,
    ): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }
}
