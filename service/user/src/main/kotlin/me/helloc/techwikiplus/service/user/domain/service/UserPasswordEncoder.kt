package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordCrypter

class UserPasswordEncoder(
    private val passwordCrypter: PasswordCrypter,
) {
    fun encode(rawPassword: RawPassword): EncodedPassword {
        return passwordCrypter.encode(rawPassword)
    }
}
