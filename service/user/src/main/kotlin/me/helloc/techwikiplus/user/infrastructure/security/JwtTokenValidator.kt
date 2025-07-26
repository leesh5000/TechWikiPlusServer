package me.helloc.techwikiplus.user.infrastructure.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import me.helloc.techwikiplus.user.domain.TokenType
import me.helloc.techwikiplus.user.domain.port.outbound.TokenValidator
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenValidator(
    private val jwtProperties: JwtProperties,
) : TokenValidator {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    override fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    override fun getTokenType(token: String): TokenType {
        val type = getClaims(token).get("type", String::class.java)
        return TokenType.from(type)
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
