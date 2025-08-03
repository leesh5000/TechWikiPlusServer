package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword

interface PasswordEncoder {
    fun encode(rawPassword: RawPassword): EncodedPassword

    fun matches(
        rawPassword: RawPassword,
        encodedPassword: EncodedPassword,
    ): Boolean
}
