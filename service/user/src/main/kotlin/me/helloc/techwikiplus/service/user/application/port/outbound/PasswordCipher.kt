package me.helloc.techwikiplus.service.user.application.port.outbound

import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword

interface PasswordCipher {
    fun encode(rawPassword: RawPassword): EncodedPassword

    fun matches(
        rawPassword: RawPassword,
        encodedPassword: EncodedPassword,
    ): Boolean
}
