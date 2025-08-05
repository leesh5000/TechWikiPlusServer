package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode

interface VerificationCodeStore {
    fun store(
        email: Email,
        code: VerificationCode,
    )

    fun exists(email: Email): Boolean

    fun get(email: Email): VerificationCode

    fun equalsOrThrows(
        email: Email,
        code: VerificationCode,
    )
}
