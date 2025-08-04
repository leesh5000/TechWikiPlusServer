package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncoder

class UserPasswordService(
    private val passwordEncoder: PasswordEncoder,
) {
    fun encode(rawPassword: RawPassword): EncodedPassword {
        return passwordEncoder.encode(rawPassword)
    }

    fun matchOrThrows(
        rawPassword: RawPassword,
        encodedPassword: EncodedPassword,
    ) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw InvalidCredentialsException()
        }
    }
}
