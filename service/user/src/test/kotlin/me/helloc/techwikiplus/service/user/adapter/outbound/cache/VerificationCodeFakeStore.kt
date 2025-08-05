package me.helloc.techwikiplus.service.user.adapter.outbound.cache

import me.helloc.techwikiplus.service.user.application.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.service.user.domain.exception.InvalidVerificationCodeException
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.MINUTES
import java.util.concurrent.ConcurrentHashMap

class VerificationCodeFakeStore : VerificationCodeStore {
    companion object {
        private const val KEY_FORMAT = "user-service:user:email:%s"
        private val TTL = Duration.of(5, MINUTES)
    }

    private val store = ConcurrentHashMap<String, CacheEntry>()

    override fun store(
        email: Email,
        code: RegistrationCode,
    ) {
        val key = KEY_FORMAT.format(email.value)
        val expiresAt = Instant.now().plus(TTL)
        store[key] = CacheEntry(code.value, expiresAt)
    }

    override fun exists(email: Email): Boolean {
        val key = KEY_FORMAT.format(email.value)
        val entry = store[key] ?: return false

        // TTL 만료 체크
        if (entry.expiresAt.isBefore(Instant.now())) {
            store.remove(key)
            return false
        }

        return true
    }

    override fun get(email: Email): RegistrationCode {
        val key = KEY_FORMAT.format(email.value)
        val entry =
            store[key]
                ?: throw InvalidVerificationCodeException(
                    "Verification code not found for email: ${email.value}",
                )

        if (entry.expiresAt.isBefore(Instant.now())) {
            store.remove(key)
            throw InvalidVerificationCodeException(
                "Verification code expired for email: ${email.value}",
            )
        }

        return RegistrationCode(entry.value)
    }

    override fun equalsOrThrows(
        email: Email,
        code: RegistrationCode,
    ) {
        if (!exists(email)) {
            throw InvalidVerificationCodeException(
                "Verification code not found for email: ${email.value}",
            )
        }

        if (get(email) != code) {
            throw InvalidVerificationCodeException(
                "Verification code is invalid for email: ${email.value}",
            )
        }
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
