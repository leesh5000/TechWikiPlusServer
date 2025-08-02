package me.helloc.techwikiplus.service.user.infrastructure.cache

import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class FakeCacheStore : VerificationCodeStore {
    private val store = ConcurrentHashMap<String, CacheEntry>()

    override fun set(
        key: String,
        value: String,
        ttlSeconds: Duration,
    ) {
        val expiresAt = Instant.now().plus(ttlSeconds)
        store[key] = CacheEntry(value, expiresAt)
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

    // 테스트용 추가 메서드
    fun get(key: String): String? {
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
