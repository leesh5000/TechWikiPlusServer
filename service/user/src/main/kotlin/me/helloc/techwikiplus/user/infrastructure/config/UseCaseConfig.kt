package me.helloc.techwikiplus.user.infrastructure.config

import me.helloc.techwikiplus.user.application.ApplicationExceptionHandler
import me.helloc.techwikiplus.user.application.RefreshTokenUseCase
import me.helloc.techwikiplus.user.application.ResendVerificationCodeUseCase
import me.helloc.techwikiplus.user.application.UserLoginUseCase
import me.helloc.techwikiplus.user.application.UserSignUpUseCase
import me.helloc.techwikiplus.user.application.VerifyEmailUseCase
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.domain.port.outbound.RefreshTokenStore
import me.helloc.techwikiplus.user.domain.port.outbound.TokenConfiguration
import me.helloc.techwikiplus.user.domain.port.outbound.TokenProvider
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.domain.service.PendingUserValidator
import me.helloc.techwikiplus.user.domain.service.TokenRefresher
import me.helloc.techwikiplus.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.domain.service.UserRegister
import me.helloc.techwikiplus.user.domain.service.UserWriter
import me.helloc.techwikiplus.user.domain.service.VerificationCodeSender
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UseCaseConfig {
    @Bean
    fun applicationExceptionHandler(): ApplicationExceptionHandler {
        return ApplicationExceptionHandler(
            LoggerFactory.getLogger(ApplicationExceptionHandler::class.java),
        )
    }

    @Bean
    fun userSignUpUseCase(
        userRegister: UserRegister,
        verificationCodeSender: VerificationCodeSender,
        exceptionHandler: ApplicationExceptionHandler,
    ): UserSignUpUseCase {
        return UserSignUpUseCase(userRegister, verificationCodeSender, exceptionHandler)
    }

    @Bean
    fun userLoginUseCase(
        userReader: UserReader,
        userAuthenticator: UserAuthenticator,
        tokenProvider: TokenProvider,
        refreshTokenStore: RefreshTokenStore,
        tokenConfiguration: TokenConfiguration,
        exceptionHandler: ApplicationExceptionHandler,
    ): UserLoginUseCase {
        return UserLoginUseCase(
            userReader = userReader,
            userAuthenticator = userAuthenticator,
            tokenProvider = tokenProvider,
            refreshTokenStore = refreshTokenStore,
            tokenConfiguration = tokenConfiguration,
            exceptionHandler = exceptionHandler,
        )
    }

    @Bean
    fun verifyEmailUseCase(
        verificationCodeStore: VerificationCodeStore,
        userReader: UserReader,
        userWriter: UserWriter,
        exceptionHandler: ApplicationExceptionHandler,
    ): VerifyEmailUseCase {
        return VerifyEmailUseCase(verificationCodeStore, userReader, userWriter, exceptionHandler)
    }

    @Bean
    fun resendVerificationCodeUseCase(
        mailSender: MailSender,
        verificationCodeStore: VerificationCodeStore,
        pendingUserValidator: PendingUserValidator,
        exceptionHandler: ApplicationExceptionHandler,
    ): ResendVerificationCodeUseCase {
        return ResendVerificationCodeUseCase(mailSender, verificationCodeStore, pendingUserValidator, exceptionHandler)
    }

    @Bean
    fun refreshTokenUseCase(
        tokenRefresher: TokenRefresher,
        exceptionHandler: ApplicationExceptionHandler,
    ): RefreshTokenUseCase {
        return RefreshTokenUseCase(tokenRefresher, exceptionHandler)
    }
}
