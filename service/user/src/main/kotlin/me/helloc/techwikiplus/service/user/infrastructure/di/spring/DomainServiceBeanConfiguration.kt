package me.helloc.techwikiplus.service.user.infrastructure.di.spring

import me.helloc.techwikiplus.domain.service.UserAuthenticationService
import me.helloc.techwikiplus.domain.service.UserPasswordService
import me.helloc.techwikiplus.domain.service.UserReader
import me.helloc.techwikiplus.domain.service.VerificationCodeMailSender
import me.helloc.techwikiplus.domain.service.port.MailSender
import me.helloc.techwikiplus.domain.service.port.PasswordEncoder
import me.helloc.techwikiplus.domain.service.port.UserRepository
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
    fun userReader(userRepository: UserRepository): UserReader {
        return UserReader(userRepository)
    }

    @Bean
    fun userPasswordService(passwordEncoder: PasswordEncoder): UserPasswordService {
        return UserPasswordService(passwordEncoder)
    }

    @Bean
    fun userAuthenticationService(
        userRepository: UserRepository,
        userPasswordService: UserPasswordService,
    ): UserAuthenticationService {
        return UserAuthenticationService(userRepository, userPasswordService)
    }

    @Bean
    fun verificationCodeMailSender(mailSender: MailSender): VerificationCodeMailSender {
        return VerificationCodeMailSender(mailSender)
    }
}
