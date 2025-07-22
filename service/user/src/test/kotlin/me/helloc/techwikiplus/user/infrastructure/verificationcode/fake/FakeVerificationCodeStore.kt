package me.helloc.techwikiplus.user.infrastructure.verificationcode.fake

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.exception.CustomException.AuthenticationException.ExpiredEmailVerification
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.VerificationCodeStore
import java.time.Duration
import java.time.LocalDateTime

class FakeVerificationCodeStore(
    private val clock: Clock = Clock.system,
) : VerificationCodeStore {
    private val store = mutableMapOf<String, StoredCode>()

    data class StoredCode(
        val code: VerificationCode,
        val expiryTime: LocalDateTime,
    )

    override fun storeWithExpiry(
        email: String,
        code: VerificationCode,
        ttl: Duration,
    ) {
        val expiryTime = clock.localDateTime().plus(ttl)
        store[email] = StoredCode(code, expiryTime)
    }

    override fun retrieveOrThrows(email: String): VerificationCode {
        val storedCode =
            store[email]
                ?: throw ExpiredEmailVerification(email)

        if (clock.localDateTime().isAfter(storedCode.expiryTime)) {
            store.remove(email)
            throw ExpiredEmailVerification(email)
        }

        return storedCode.code
    }

    fun clear() {
        store.clear()
    }

    fun contains(email: String): Boolean = store.containsKey(email)
}
