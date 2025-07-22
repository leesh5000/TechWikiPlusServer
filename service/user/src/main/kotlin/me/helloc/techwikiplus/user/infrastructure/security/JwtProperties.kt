package me.helloc.techwikiplus.user.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    var secret: String = "techwikiplus-jwt-secret-key-for-authentication-2024",
    // 1 hour in milliseconds
    var accessTokenExpiration: Long = 3600000,
    // 7 days in milliseconds
    var refreshTokenExpiration: Long = 604800000,
)
