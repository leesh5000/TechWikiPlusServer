package me.helloc.techwikiplus.user.infrastructure.config

import me.helloc.techwikiplus.user.domain.port.outbound.IdGenerator
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.domain.port.outbound.PasswordEncoder
import me.helloc.techwikiplus.user.domain.port.outbound.PasswordValidator
import me.helloc.techwikiplus.user.domain.port.outbound.RefreshTokenStore
import me.helloc.techwikiplus.user.domain.port.outbound.TokenConfiguration
import me.helloc.techwikiplus.user.domain.port.outbound.TokenProvider
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.domain.service.PendingUserValidator
import me.helloc.techwikiplus.user.domain.service.TokenRefresher
import me.helloc.techwikiplus.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.user.domain.service.UserDuplicateChecker
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.domain.service.UserRegister
import me.helloc.techwikiplus.user.domain.service.UserWriter
import me.helloc.techwikiplus.user.domain.service.VerificationCodeSender
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainServiceConfig {
    @Bean
    fun userRegister(
        userWriter: UserWriter,
        userDuplicateChecker: UserDuplicateChecker,
        passwordValidator: PasswordValidator,
        passwordEncoder: PasswordEncoder,
        idGenerator: IdGenerator,
    ): UserRegister {
        return UserRegister(
            userWriter = userWriter,
            userDuplicateChecker = userDuplicateChecker,
            passwordValidator = passwordValidator,
            passwordEncoder = passwordEncoder,
            idGenerator = idGenerator,
        )
    }

    @Bean
    fun userAuthenticator(passwordEncoder: PasswordEncoder): UserAuthenticator {
        return UserAuthenticator(passwordEncoder)
    }

    @Bean
    fun userReader(repository: UserRepository): UserReader {
        return UserReader(repository)
    }

    @Bean
    fun userWriter(repository: UserRepository): UserWriter {
        return UserWriter(repository)
    }

    @Bean
    fun userDuplicateChecker(repository: UserRepository): UserDuplicateChecker {
        return UserDuplicateChecker(repository)
    }

    @Bean
    fun pendingUserValidator(repository: UserRepository): PendingUserValidator {
        return PendingUserValidator(repository)
    }

    @Bean
    fun verificationCodeSender(
        mailSender: MailSender,
        verificationCodeStore: VerificationCodeStore,
    ): VerificationCodeSender {
        return VerificationCodeSender(mailSender, verificationCodeStore)
    }

    @Bean
    fun tokenRefresher(
        tokenProvider: TokenProvider,
        refreshTokenStore: RefreshTokenStore,
        tokenConfiguration: TokenConfiguration,
    ): TokenRefresher {
        return TokenRefresher(tokenProvider, refreshTokenStore, tokenConfiguration)
    }
}
