package me.helloc.techwikiplus.service.user.domain.port

import me.helloc.techwikiplus.service.user.domain.service.port.CacheStore
import java.time.Duration
import java.time.Instant

open class FakeCacheStore : CacheStore {
    data class CacheEntry(
        val value: String,
        val expiresAt: Instant?,
    )

    protected val cache = mutableMapOf<String, CacheEntry>()
    private var currentTime = Instant.now()

    override fun get(key: String): String? {
        val entry = cache[key] ?: return null

        if (entry.expiresAt != null && currentTime.isAfter(entry.expiresAt)) {
            cache.remove(key)
            return null
        }

        return entry.value
    }

    override fun put(
        key: String,
        value: String,
        ttl: Duration,
    ) {
        val expiresAt =
            if (ttl == Duration.ZERO) {
                null
            } else {
                currentTime.plus(ttl)
            }

        cache[key] = CacheEntry(value, expiresAt)
    }

    override fun delete(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }

    fun setCurrentTime(time: Instant) {
        this.currentTime = time
    }

    fun advanceTimeBy(duration: Duration) {
        this.currentTime = currentTime.plus(duration)
    }

    fun contains(key: String): Boolean {
        return get(key) != null
    }

    fun size(): Int {
        return cache.size
    }
}
