package me.helloc.techwikiplus.service.user.domain.service.port

interface TokenGenerator {
    fun generateAccessToken(userId: String): String

    fun generateRefreshToken(userId: String): String
}
