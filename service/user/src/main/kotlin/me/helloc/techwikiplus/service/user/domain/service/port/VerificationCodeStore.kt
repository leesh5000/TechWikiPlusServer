package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.RegistrationCode

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
