package me.helloc.techwikiplus.service.user.domain.port

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode

interface VerificationCodeStore {
    fun store(
        email: Email,
        code: RegistrationCode,
    )

    fun exists(email: Email): Boolean

    fun get(email: Email): RegistrationCode

    fun equalsOrThrows(
        email: Email,
        code: RegistrationCode,
    )
}
