package me.helloc.techwikiplus.service.user.infrastructure.token

import me.helloc.techwikiplus.service.user.domain.exception.ExpiredTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenTypeException
import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import me.helloc.techwikiplus.service.user.domain.service.port.TokenValidator
import java.time.Instant

class FakeTokenValidator(
    private val clockHolder: ClockHolder? = null,
) : TokenValidator {
    private val validTokens = mutableMapOf<String, TokenValidator.TokenClaims>()

    fun addValidToken(
        token: String,
        claims: TokenValidator.TokenClaims,
    ) {
        validTokens[token] = claims
    }

    fun addValidRefreshToken(
        token: String,
        userId: String,
    ) {
        val now = clockHolder?.now() ?: Instant.now()
        validTokens[token] =
            TokenValidator.TokenClaims(
                userId = userId,
                tokenType = "refresh",
                issuedAt = now.minusSeconds(3600),
                expiresAt = now.plusSeconds(86400),
            )
    }

    override fun validateRefreshTokenOrThrows(token: String): TokenValidator.TokenClaims {
        val claims =
            validTokens[token]
                ?: throw InvalidTokenException("Invalid token format")

        val currentTime = clockHolder?.now() ?: Instant.now()

        // Check if token is expired
        if (claims.expiresAt.isBefore(currentTime)) {
            throw ExpiredTokenException()
        }

        // Check if token type is refresh
        if (claims.tokenType != "refresh") {
            throw InvalidTokenTypeException(expected = "refresh", actual = claims.tokenType)
        }

        return claims
    }
}
