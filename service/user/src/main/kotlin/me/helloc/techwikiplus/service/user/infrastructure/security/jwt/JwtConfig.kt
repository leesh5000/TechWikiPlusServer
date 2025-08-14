package me.helloc.techwikiplus.service.user.infrastructure.security.jwt

import me.helloc.techwikiplus.service.user.domain.service.port.TokenManager
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig {
    @Bean
    fun jwtTokenManager(jwtProperties: JwtProperties): TokenManager {
        return JwtTokenManager(
            secret = jwtProperties.secret,
            accessTokenValidityInSeconds = jwtProperties.accessTokenValidityInSeconds,
            refreshTokenValidityInSeconds = jwtProperties.refreshTokenValidityInSeconds,
        )
    }
}
