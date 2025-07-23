package me.helloc.techwikiplus.user.domain.service

interface TokenConfiguration {
    val accessTokenExpiration: Long
    val refreshTokenExpiration: Long
}
