package me.helloc.techwikiplus.service.user.infrastructure.di.spring

import me.helloc.techwikiplus.service.user.domain.service.Auditor
import me.helloc.techwikiplus.service.user.domain.service.PasswordConfirmationVerifier
import me.helloc.techwikiplus.service.user.domain.service.TokenService
import me.helloc.techwikiplus.service.user.domain.service.UserEmailVerificationCodeManager
import me.helloc.techwikiplus.service.user.domain.service.UserPasswordService
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserWriter
import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import me.helloc.techwikiplus.service.user.domain.service.port.EmailTemplateService
import me.helloc.techwikiplus.service.user.domain.service.port.MailSender
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncoder
import me.helloc.techwikiplus.service.user.domain.service.port.TokenGenerator
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 도메인 서비스를 Spring Bean으로 등록하는 Configuration
 *
 * 도메인 계층의 순수성을 유지하면서 Spring DI를 활용하기 위해
 * Infrastructure 계층에서 도메인 서비스들을 Bean으로 등록합니다.
 */
@Configuration
class DomainServiceBeanConfiguration {
    @Bean
    fun userWriter(userRepository: UserRepository): UserWriter {
        return UserWriter(userRepository)
    }

    @Bean
    fun userReader(userRepository: UserRepository): UserReader {
        return UserReader(userRepository)
    }

    @Bean
    fun userPasswordService(passwordEncoder: PasswordEncoder): UserPasswordService {
        return UserPasswordService(passwordEncoder)
    }

    @Bean
    fun auditor(clockHolder: ClockHolder): Auditor {
        return Auditor(clockHolder)
    }

    @Bean
    fun emailVerificationCodeSender(
        mailSender: MailSender,
        codeStore: VerificationCodeStore,
        emailTemplateService: EmailTemplateService,
    ): UserEmailVerificationCodeManager {
        return UserEmailVerificationCodeManager(mailSender, codeStore, emailTemplateService)
    }

    @Bean
    fun passwordConfirmationVerifier(): PasswordConfirmationVerifier {
        return PasswordConfirmationVerifier()
    }

    @Bean
    fun tokenService(
        tokenGenerator: TokenGenerator,
        clockHolder: ClockHolder,
    ): TokenService {
        return TokenService(tokenGenerator, clockHolder)
    }
}
