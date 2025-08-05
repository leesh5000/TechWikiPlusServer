package me.helloc.techwikiplus.service.user.adapter.outbound.cache

import me.helloc.techwikiplus.service.user.application.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.service.user.domain.exception.InvalidVerificationCodeException
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.temporal.ChronoUnit.MINUTES

@Component
class VerificationCodeRedisStore(
    private val template: StringRedisTemplate,
) : VerificationCodeStore {
    companion object {
        private const val KEY_FORMAT = "user-service:user:email:%s"
        private val TTL = Duration.of(5, MINUTES)
    }

    override fun store(
        email: Email,
        code: RegistrationCode,
    ) {
        val key = KEY_FORMAT.format(email.value)
        template.opsForValue().set(key, code.value, TTL)
    }

    override fun exists(email: Email): Boolean {
        val key = KEY_FORMAT.format(email.value)
        return template.hasKey(key)
    }

    override fun get(email: Email): RegistrationCode {
        val key = KEY_FORMAT.format(email.value)
        val value =
            template.opsForValue().get(key)
                ?: throw InvalidVerificationCodeException("Verification code not found for email: ${email.value}")
        return RegistrationCode(value)
    }

    override fun equalsOrThrows(
        email: Email,
        code: RegistrationCode,
    ) {
        if (!exists(email)) {
            throw InvalidVerificationCodeException("Verification code not found for email: ${email.value}")
        }
        if (get(email) != code) {
            throw InvalidVerificationCodeException("Invalid verification code for email: ${email.value}")
        }
    }
}
