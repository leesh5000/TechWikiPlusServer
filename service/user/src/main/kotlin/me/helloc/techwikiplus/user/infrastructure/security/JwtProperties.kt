package me.helloc.techwikiplus.user.infrastructure.security

import me.helloc.techwikiplus.user.domain.service.TokenConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    var secret: String = "techwikiplus-jwt-secret-key-for-authentication-2024",
    // 1 hour in milliseconds
    override var accessTokenExpiration: Long = 3600000,
    // 7 days in milliseconds
    override var refreshTokenExpiration: Long = 604800000,
) : TokenConfiguration
