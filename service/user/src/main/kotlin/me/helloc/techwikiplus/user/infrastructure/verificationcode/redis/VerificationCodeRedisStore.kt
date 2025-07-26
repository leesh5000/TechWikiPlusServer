package me.helloc.techwikiplus.user.infrastructure.verificationcode.redis

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.exception.authentication.ExpiredEmailVerificationException
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.infrastructure.exception.ExternalServiceException
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
        try {
            val key = RedisKeyFormatter.verificationCodeKey(email)
            redisTemplate.opsForValue().set(key, code.value, ttl)
        } catch (e: Exception) {
            throw ExternalServiceException("Redis", e)
        }
    }

    override fun retrieveOrThrows(email: String): VerificationCode {
        try {
            val key = RedisKeyFormatter.verificationCodeKey(email)
            return redisTemplate.opsForValue().get(key)
                ?.let { VerificationCode(it) }
                ?: throw ExpiredEmailVerificationException(email)
        } catch (e: ExpiredEmailVerificationException) {
            throw e // 도메인 예외는 그대로 전파
        } catch (e: Exception) {
            throw ExternalServiceException("Redis", e)
        }
    }
}
