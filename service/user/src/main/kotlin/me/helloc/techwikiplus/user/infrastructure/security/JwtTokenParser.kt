package me.helloc.techwikiplus.user.infrastructure.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import me.helloc.techwikiplus.user.domain.port.outbound.TokenParser
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtTokenParser(
    private val jwtProperties: JwtProperties,
) : TokenParser {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    override fun getEmailFromToken(token: String): String {
        return getClaims(token).subject
    }

    override fun getUserIdFromToken(token: String): Long {
        val claims = getClaims(token)
        return when (val userId = claims["userId"]) {
            is Long -> userId
            is Int -> userId.toLong()
            is String -> userId.toLong()
            else -> throw IllegalStateException("userId claim is not a valid numeric type")
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
