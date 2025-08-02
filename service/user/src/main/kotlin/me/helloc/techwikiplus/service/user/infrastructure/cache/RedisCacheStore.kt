package me.helloc.techwikiplus.service.user.infrastructure.cache

import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

class RedisCacheStore(
    private val template: StringRedisTemplate,
) : VerificationCodeStore {
    override fun set(
        key: String,
        value: String,
        ttlSeconds: Duration,
    ) {
        template.opsForValue().set(key, value, ttlSeconds)
    }

    override fun exists(key: String): Boolean {
        return template.hasKey(key)
    }
}
