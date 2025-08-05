package me.helloc.techwikiplus.service.user.application.port.outbound

interface TokenGenerator {
    fun generateAccessToken(userId: String): String

    fun generateRefreshToken(userId: String): String
}
