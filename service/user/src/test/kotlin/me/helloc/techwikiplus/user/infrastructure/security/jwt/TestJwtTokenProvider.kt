package me.helloc.techwikiplus.user.infrastructure.security.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
import java.util.Date
import javax.crypto.SecretKey

class TestJwtTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    fun createExpiredRefreshToken(
        email: String,
        userId: Long,
    ): String {
        val now = Date()
        // 1초 전에 만료된 토큰 생성
        val expiryDate = Date(now.time - 1000)

        return Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .claim("type", "refresh")
            .issuedAt(Date(now.time - jwtProperties.refreshTokenExpiration))
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }
}
