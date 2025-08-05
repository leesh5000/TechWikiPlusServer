package me.helloc.techwikiplus.service.user.adapter.outbound.di.spring

import me.helloc.techwikiplus.service.user.application.port.outbound.ClockHolder
import me.helloc.techwikiplus.service.user.application.port.outbound.EmailTemplatePrinter
import me.helloc.techwikiplus.service.user.application.port.outbound.MailSender
import me.helloc.techwikiplus.service.user.application.port.outbound.PasswordCipher
import me.helloc.techwikiplus.service.user.application.port.outbound.TokenGenerator
import me.helloc.techwikiplus.service.user.application.port.outbound.TokenValidator
import me.helloc.techwikiplus.service.user.application.port.outbound.UserRepository
import me.helloc.techwikiplus.service.user.application.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.service.user.domain.service.Auditor
import me.helloc.techwikiplus.service.user.domain.service.PasswordConfirmationVerifier
import me.helloc.techwikiplus.service.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.service.user.domain.service.UserEmailVerificationCodeManager
import me.helloc.techwikiplus.service.user.domain.service.UserPasswordEncoder
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserTokenGenerator
import me.helloc.techwikiplus.service.user.domain.service.UserTokenValidator
import me.helloc.techwikiplus.service.user.domain.service.UserWriter
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
    fun userPasswordEncoder(passwordCipher: PasswordCipher): UserPasswordEncoder {
        return UserPasswordEncoder(passwordCipher)
    }

    @Bean
    fun auditor(clockHolder: ClockHolder): Auditor {
        return Auditor(clockHolder)
    }

    @Bean
    fun emailVerificationCodeSender(
        mailSender: MailSender,
        codeStore: VerificationCodeStore,
        emailTemplatePrinter: EmailTemplatePrinter,
    ): UserEmailVerificationCodeManager {
        return UserEmailVerificationCodeManager(mailSender, codeStore, emailTemplatePrinter)
    }

    @Bean
    fun passwordConfirmationVerifier(): PasswordConfirmationVerifier {
        return PasswordConfirmationVerifier()
    }

    @Bean
    fun userTokenGenerator(
        tokenGenerator: TokenGenerator,
        clockHolder: ClockHolder,
    ): UserTokenGenerator {
        return UserTokenGenerator(tokenGenerator, clockHolder)
    }

    @Bean
    fun userTokenValidator(tokenValidator: TokenValidator): UserTokenValidator {
        return UserTokenValidator(tokenValidator)
    }

    @Bean
    fun userAuthenticator(crypter: PasswordCipher): UserAuthenticator {
        return UserAuthenticator(crypter)
    }
}
