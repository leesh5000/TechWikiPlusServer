package me.helloc.techwikiplus.service.user.adapter.outbound.token.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    // 1 hour
    val accessTokenValidityInSeconds: Long = 3600,
    // 30 days
    val refreshTokenValidityInSeconds: Long = 2592000,
)
