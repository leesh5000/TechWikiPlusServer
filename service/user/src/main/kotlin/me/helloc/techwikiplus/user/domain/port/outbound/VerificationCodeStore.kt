package me.helloc.techwikiplus.user.domain.port.outbound

import me.helloc.techwikiplus.user.domain.VerificationCode
import java.time.Duration

interface VerificationCodeStore {
    fun storeWithExpiry(
        email: String,
        code: VerificationCode,
        ttl: Duration,
    )

    fun retrieveOrThrows(email: String): VerificationCode
}
