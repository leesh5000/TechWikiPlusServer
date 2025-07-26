package me.helloc.techwikiplus.user.domain.port.outbound

interface TokenConfiguration {
    val accessTokenExpiration: Long
    val refreshTokenExpiration: Long
}
