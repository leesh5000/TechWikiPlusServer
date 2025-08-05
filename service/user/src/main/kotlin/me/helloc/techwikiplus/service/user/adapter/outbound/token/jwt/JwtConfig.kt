package me.helloc.techwikiplus.service.user.adapter.outbound.token.jwt

import me.helloc.techwikiplus.service.user.application.port.outbound.TokenGenerator
import me.helloc.techwikiplus.service.user.application.port.outbound.TokenValidator
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig {
    @Bean
    fun tokenGenerator(jwtProperties: JwtProperties): TokenGenerator {
        return JwtTokenGenerator(
            secret = jwtProperties.secret,
            accessTokenValidityInSeconds = jwtProperties.accessTokenValidityInSeconds,
            refreshTokenValidityInSeconds = jwtProperties.refreshTokenValidityInSeconds,
        )
    }

    @Bean
    fun tokenValidator(jwtProperties: JwtProperties): TokenValidator {
        return JwtTokenValidator(
            secret = jwtProperties.secret,
        )
    }
}
