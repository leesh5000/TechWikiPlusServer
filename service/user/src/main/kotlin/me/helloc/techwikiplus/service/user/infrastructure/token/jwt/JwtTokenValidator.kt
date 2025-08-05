package me.helloc.techwikiplus.service.user.infrastructure.token.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import me.helloc.techwikiplus.service.user.domain.exception.ExpiredTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenTypeException
import me.helloc.techwikiplus.service.user.domain.service.port.TokenValidator
import java.time.Instant

class JwtTokenValidator(
    secret: String,
) : TokenValidator {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    override fun validateRefreshTokenOrThrows(token: String): TokenValidator.TokenClaims {
        val claims = parseToken(token)
        val tokenType = claims["token_type"] as? String ?: throw InvalidTokenException("Missing token type")

        if (tokenType != "refresh") {
            throw InvalidTokenTypeException(expected = "refresh", actual = tokenType)
        }

        return TokenValidator.TokenClaims(
            userId = claims.subject,
            tokenType = tokenType,
            issuedAt = Instant.ofEpochMilli(claims.issuedAt.time),
            expiresAt = Instant.ofEpochMilli(claims.expiration.time),
        )
    }

    private fun parseToken(token: String): Claims {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: ExpiredJwtException) {
            throw ExpiredTokenException("Token has expired")
        } catch (e: JwtException) {
            throw InvalidTokenException("Invalid token: ${e.message}")
        } catch (e: Exception) {
            throw InvalidTokenException("Invalid token format")
        }
    }
}
