package me.helloc.techwikiplus.service.user.infrastructure.cache

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.UserEmailVerificationCodeManager
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class FakeCacheStore : VerificationCodeStore {
    private val store = ConcurrentHashMap<String, CacheEntry>()

    override fun set(
        key: String,
        code: VerificationCode,
        ttlSeconds: Duration,
    ) {
        val expiresAt = Instant.now().plus(ttlSeconds)
        store[key] = CacheEntry(code.value, expiresAt)
    }

    override fun exists(key: String): Boolean {
        val entry = store[key] ?: return false

        // TTL 만료 체크
        if (entry.expiresAt.isBefore(Instant.now())) {
            store.remove(key)
            return false
        }

        return true
    }

    override fun get(email: Email): VerificationCode {
        val key = UserEmailVerificationCodeManager.EMAIL_VERIFICATION_CODE_KEY_FORMAT.format(email.value)
        val entry =
            store[key]
                ?: throw me.helloc.techwikiplus.service.user.domain.exception.InvalidVerificationCodeException(
                    "Verification code not found for email: ${email.value}",
                )

        if (entry.expiresAt.isBefore(Instant.now())) {
            store.remove(key)
            throw me.helloc.techwikiplus.service.user.domain.exception.InvalidVerificationCodeException(
                "Verification code expired for email: ${email.value}",
            )
        }

        return VerificationCode(entry.value)
    }

    private fun getByKey(key: String): String? {
        val entry = store[key] ?: return null

        if (entry.expiresAt.isBefore(Instant.now())) {
            store.remove(key)
            return null
        }

        return entry.value
    }

    fun clear() {
        store.clear()
    }

    fun size(): Int = store.size

    fun getTtl(key: String): Duration? {
        val entry = store[key] ?: return null
        val now = Instant.now()

        if (entry.expiresAt.isBefore(now)) {
            store.remove(key)
            return null
        }

        return Duration.between(now, entry.expiresAt)
    }

    private data class CacheEntry(
        val value: String,
        val expiresAt: Instant,
    )
}
