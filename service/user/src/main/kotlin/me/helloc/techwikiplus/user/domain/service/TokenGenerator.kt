package me.helloc.techwikiplus.user.domain.service

interface TokenGenerator {
    fun createAccessToken(
        email: String,
        userId: Long,
    ): String

    fun createRefreshToken(
        email: String,
        userId: Long,
    ): String
}
