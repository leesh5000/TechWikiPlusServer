package me.helloc.techwikiplus.service.user.infrastructure.security.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.UserToken
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.service.port.TokenManager
import java.util.Date

class JwtTokenManager(
    secret: String,
    private val accessTokenValidityInSeconds: Long = 3600,
    private val refreshTokenValidityInSeconds: Long = 2592000,
) : TokenManager {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    override fun generateAccessToken(userId: UserId): UserToken {
        val now = Date()
        val expiration = Date(now.time + accessTokenValidityInSeconds * 1000)

        val token: String =
            Jwts.builder()
                .setSubject(userId.value.toString())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("token_type", "access")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact()
        return UserToken(
            userId = userId,
            token = token,
            expiresAt = expiration.time,
        )
    }

    override fun generateRefreshToken(userId: UserId): UserToken {
        val now = Date()
        val expiration = Date(now.time + refreshTokenValidityInSeconds * 1000)
        val token: String =
            Jwts.builder()
                .setSubject(userId.value.toString())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("token_type", "refresh")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact()
        return UserToken(
            userId = userId,
            token = token,
            expiresAt = expiration.time,
        )
    }

    override fun validateRefreshToken(
        userId: UserId,
        refreshToken: String,
    ): UserId {
        try {
            val claims =
                Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .body

            // 토큰 타입과 userId 검증만 수행
            val tokenType =
                claims["token_type"] as? String
                    ?: throw DomainException(ErrorCode.INVALID_TOKEN, arrayOf("Missing token type"))

            if (tokenType != "refresh") {
                throw DomainException(ErrorCode.INVALID_TOKEN_TYPE, arrayOf(tokenType))
            }

            val tokenUserId = UserId.from(claims.subject)
            if (tokenUserId != userId) {
                throw DomainException(ErrorCode.INVALID_TOKEN, arrayOf("User ID mismatch"))
            }
            return tokenUserId
        } catch (e: ExpiredJwtException) {
            throw DomainException(ErrorCode.TOKEN_EXPIRED, arrayOf("Refresh token"))
        }
    }

    override fun validateAccessToken(token: String): UserId {
        try {
            val claims =
                Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .body

            val tokenType =
                claims["token_type"] as? String
                    ?: throw DomainException(ErrorCode.INVALID_TOKEN, arrayOf("Missing token type"))

            if (tokenType != "access") {
                throw DomainException(ErrorCode.INVALID_TOKEN_TYPE, arrayOf(tokenType))
            }

            return UserId.from(claims.subject)
        } catch (e: ExpiredJwtException) {
            throw DomainException(ErrorCode.TOKEN_EXPIRED, arrayOf("Access token"))
        }
    }
}
