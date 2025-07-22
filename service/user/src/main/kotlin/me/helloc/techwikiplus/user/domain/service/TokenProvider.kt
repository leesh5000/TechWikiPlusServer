package me.helloc.techwikiplus.user.domain.service

interface TokenProvider {
    fun createAccessToken(
        email: String,
        userId: Long,
    ): String

    fun createRefreshToken(
        email: String,
        userId: Long,
    ): String

    fun validateToken(token: String): Boolean

    fun getEmailFromToken(token: String): String

    fun getUserIdFromToken(token: String): Long

    fun getTokenType(token: String): String
}
