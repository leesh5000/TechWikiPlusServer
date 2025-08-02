package me.helloc.techwikiplus.service.user.infrastructure.cache

import me.helloc.techwikiplus.service.user.domain.exception.InvalidVerificationCodeException
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.UserEmailVerificationCodeManager
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

class RedisCacheStore(
    private val template: StringRedisTemplate,
) : VerificationCodeStore {
    override fun set(
        key: String,
        code: VerificationCode,
        ttlSeconds: Duration,
    ) {
        template.opsForValue().set(key, code.value, ttlSeconds)
    }

    override fun exists(key: String): Boolean {
        return template.hasKey(key)
    }

    override fun get(email: Email): VerificationCode {
        val key = UserEmailVerificationCodeManager.EMAIL_VERIFICATION_CODE_KEY_FORMAT.format(email.value)
        val value =
            template.opsForValue().get(key)
                ?: throw InvalidVerificationCodeException("Verification code not found for email: ${email.value}")
        return VerificationCode(value)
    }
}
