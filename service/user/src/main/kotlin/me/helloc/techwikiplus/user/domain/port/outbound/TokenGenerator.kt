package me.helloc.techwikiplus.user.domain.port.outbound

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
