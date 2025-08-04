package me.helloc.techwikiplus.service.user.infrastructure.token.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import me.helloc.techwikiplus.service.user.domain.service.port.TokenGenerator
import java.util.Date

class JwtTokenGenerator(
    private val secret: String,
    private val accessTokenValidityInSeconds: Long = 3600,
    private val refreshTokenValidityInSeconds: Long = 2592000,
) : TokenGenerator {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    override fun generateAccessToken(userId: String): String {
        val now = Date()
        val expiration = Date(now.time + accessTokenValidityInSeconds * 1000)

        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .claim("token_type", "access")
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun generateRefreshToken(userId: String): String {
        val now = Date()
        val expiration = Date(now.time + refreshTokenValidityInSeconds * 1000)

        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .claim("token_type", "refresh")
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }
}
