package me.helloc.techwikiplus.user.infrastructure.config

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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UseCaseConfig {
    @Bean
    fun userSignUpUseCase(
        userRegister: UserRegister,
        verificationCodeSender: VerificationCodeSender,
    ): UserSignUpUseCase {
        return UserSignUpUseCase(userRegister, verificationCodeSender)
    }

    @Bean
    fun userLoginUseCase(
        userReader: UserReader,
        userAuthenticator: UserAuthenticator,
        tokenProvider: TokenProvider,
        refreshTokenStore: RefreshTokenStore,
        tokenConfiguration: TokenConfiguration,
    ): UserLoginUseCase {
        return UserLoginUseCase(
            userReader = userReader,
            userAuthenticator = userAuthenticator,
            tokenProvider = tokenProvider,
            refreshTokenStore = refreshTokenStore,
            tokenConfiguration = tokenConfiguration,
        )
    }

    @Bean
    fun verifyEmailUseCase(
        verificationCodeStore: VerificationCodeStore,
        userReader: UserReader,
        userWriter: UserWriter,
    ): VerifyEmailUseCase {
        return VerifyEmailUseCase(verificationCodeStore, userReader, userWriter)
    }

    @Bean
    fun resendVerificationCodeUseCase(
        mailSender: MailSender,
        verificationCodeStore: VerificationCodeStore,
        pendingUserValidator: PendingUserValidator,
    ): ResendVerificationCodeUseCase {
        return ResendVerificationCodeUseCase(mailSender, verificationCodeStore, pendingUserValidator)
    }

    @Bean
    fun refreshTokenUseCase(tokenRefresher: TokenRefresher): RefreshTokenUseCase {
        return RefreshTokenUseCase(tokenRefresher)
    }
}
