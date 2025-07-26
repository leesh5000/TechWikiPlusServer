package me.helloc.techwikiplus.user.infrastructure.verificationcode.redis

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.exception.CustomException.AuthenticationException.ExpiredEmailVerification
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.infrastructure.redis.RedisKeyFormatter
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class VerificationCodeRedisStore(
    private val redisTemplate: StringRedisTemplate,
) : VerificationCodeStore {
    companion object {
        private const val ATTEMPT_TTL_SECONDS = 30 // 30 seconds for each attempt
    }

    override fun storeWithExpiry(
        email: String,
        code: VerificationCode,
        ttl: Duration,
    ) {
        val key = RedisKeyFormatter.verificationCodeKey(email)
        redisTemplate.opsForValue().set(key, code.value, ttl)
    }

    override fun retrieveOrThrows(email: String): VerificationCode {
        val key = RedisKeyFormatter.verificationCodeKey(email)
        return redisTemplate.opsForValue().get(key)
            ?.let { VerificationCode(it) }
            ?: throw ExpiredEmailVerification(email)
    }
}
