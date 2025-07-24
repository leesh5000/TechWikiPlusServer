package me.helloc.techwikiplus.user.application

data class TokenResult(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
)
