package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.application.port.outbound.PasswordCipher
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword

class UserPasswordEncoder(
    private val passwordCipher: PasswordCipher,
) {
    fun encode(rawPassword: RawPassword): EncodedPassword {
        return passwordCipher.encode(rawPassword)
    }
}
