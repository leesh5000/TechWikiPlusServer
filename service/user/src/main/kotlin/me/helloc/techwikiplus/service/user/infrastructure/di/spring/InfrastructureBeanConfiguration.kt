package me.helloc.techwikiplus.service.user.infrastructure.di.spring

import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import me.helloc.techwikiplus.service.user.domain.service.port.MailSender
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncoder
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore
import me.helloc.techwikiplus.service.user.infrastructure.cache.VerificationCodeRedisStore
import me.helloc.techwikiplus.service.user.infrastructure.clock.SystemClockHolder
import me.helloc.techwikiplus.service.user.infrastructure.mail.JavaMailSender
import me.helloc.techwikiplus.service.user.infrastructure.persistence.UserRepositoryImpl
import me.helloc.techwikiplus.service.user.infrastructure.security.BCryptPasswordEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class InfrastructureBeanConfiguration {
    @Bean
    fun userRepository(): UserRepository {
        return UserRepositoryImpl()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun clockHolder(): ClockHolder {
        return SystemClockHolder()
    }

    @Bean
    fun mailSender(): MailSender {
        return JavaMailSender()
    }

    @Bean
    fun userCacheStore(template: StringRedisTemplate): VerificationCodeStore {
        return VerificationCodeRedisStore(template)
    }
}
