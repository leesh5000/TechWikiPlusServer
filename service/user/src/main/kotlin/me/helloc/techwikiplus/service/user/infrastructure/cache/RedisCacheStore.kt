package me.helloc.techwikiplus.service.user.infrastructure.cache

import me.helloc.techwikiplus.service.user.domain.service.port.CacheStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisCacheStore(
    private val template: StringRedisTemplate,
) : CacheStore {
    companion object {
        private const val KEY_PREFIX_FORMAT = "service:user:%s"
    }

    override fun get(key: String): String? {
        val formattedKey = KEY_PREFIX_FORMAT.format(key)
        return template.opsForValue().get(formattedKey)
    }

    override fun put(
        key: String,
        value: String,
        ttl: Duration,
    ) {
        val formattedKey = KEY_PREFIX_FORMAT.format(key)
        template.opsForValue().set(formattedKey, value, ttl)
    }

    override fun delete(key: String) {
        val formattedKey = KEY_PREFIX_FORMAT.format(key)
        template.delete(formattedKey)
    }
}
