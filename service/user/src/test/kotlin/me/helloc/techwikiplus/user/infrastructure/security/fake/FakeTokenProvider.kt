package me.helloc.techwikiplus.user.infrastructure.security.fake

import me.helloc.techwikiplus.user.domain.TokenType
import me.helloc.techwikiplus.user.domain.port.outbound.TokenProvider

class FakeTokenProvider : TokenProvider {
    private val tokens = mutableMapOf<String, TokenData>()
    private var tokenCounter = 0L

    data class TokenData(
        val email: String,
        val userId: Long,
        val type: String,
        val valid: Boolean = true,
    )

    override fun createAccessToken(
        email: String,
        userId: Long,
    ): String {
        val token = "access_${email}_${userId}_${System.currentTimeMillis()}_${tokenCounter++}"
        tokens[token] = TokenData(email, userId, "access")
        return token
    }

    override fun createRefreshToken(
        email: String,
        userId: Long,
    ): String {
        val token = "refresh_${email}_${userId}_${System.currentTimeMillis()}_${tokenCounter++}"
        tokens[token] = TokenData(email, userId, "refresh")
        return token
    }

    override fun validateToken(token: String): Boolean {
        return tokens[token]?.valid ?: false
    }

    override fun getEmailFromToken(token: String): String {
        return tokens[token]?.email
            ?: throw IllegalArgumentException("Invalid token")
    }

    override fun getUserIdFromToken(token: String): Long {
        return tokens[token]?.userId
            ?: throw IllegalArgumentException("Invalid token")
    }

    override fun getTokenType(token: String): TokenType {
        val type =
            tokens[token]?.type
                ?: throw IllegalArgumentException("Invalid token")
        return TokenType.from(type)
    }

    fun createExpiredRefreshToken(
        email: String,
        userId: Long,
    ): String {
        val token = "expired_refresh_${email}_${userId}_${System.currentTimeMillis()}_${tokenCounter++}"
        tokens[token] = TokenData(email, userId, "refresh", valid = false)
        return token
    }

    fun invalidateToken(token: String) {
        tokens[token]?.let {
            tokens[token] = it.copy(valid = false)
        }
    }

    fun clear() {
        tokens.clear()
    }
}
