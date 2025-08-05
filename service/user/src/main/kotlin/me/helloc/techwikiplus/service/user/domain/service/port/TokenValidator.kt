package me.helloc.techwikiplus.service.user.domain.service.port

import java.time.Instant

interface TokenValidator {
    fun validateRefreshTokenOrThrows(token: String): TokenClaims

    data class TokenClaims(
        val userId: String,
        val tokenType: String,
        val issuedAt: Instant,
        val expiresAt: Instant,
    )
}
