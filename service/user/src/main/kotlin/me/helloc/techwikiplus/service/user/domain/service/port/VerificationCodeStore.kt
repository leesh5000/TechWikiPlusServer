package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import java.time.Duration

interface VerificationCodeStore {
    fun set(
        key: String,
        code: VerificationCode,
        ttlSeconds: Duration,
    )

    fun exists(key: String): Boolean

    fun get(email: Email): VerificationCode
}
