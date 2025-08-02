package me.helloc.techwikiplus.service.user.domain.service.port

import java.time.Duration

interface VerificationCodeStore {
    fun set(
        key: String,
        value: String,
        ttlSeconds: Duration,
    )

    fun exists(key: String): Boolean
}
