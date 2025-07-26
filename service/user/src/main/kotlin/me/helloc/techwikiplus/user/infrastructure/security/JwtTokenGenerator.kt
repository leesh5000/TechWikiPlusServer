package me.helloc.techwikiplus.user.infrastructure.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import me.helloc.techwikiplus.user.domain.DomainConstants
import me.helloc.techwikiplus.user.domain.port.outbound.TokenGenerator
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenGenerator(
    private val jwtProperties: JwtProperties,
) : TokenGenerator {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    override fun createAccessToken(
        email: String,
        userId: Long,
    ): String {
        return createToken(
            email = email,
            userId = userId,
            type = DomainConstants.Token.ACCESS_TYPE,
            expiration = jwtProperties.accessTokenExpiration,
            additionalClaims = emptyMap(),
        )
    }

    override fun createRefreshToken(
        email: String,
        userId: Long,
    ): String {
        return createToken(
            email = email,
            userId = userId,
            type = DomainConstants.Token.REFRESH_TYPE,
            expiration = jwtProperties.refreshTokenExpiration,
            additionalClaims = mapOf("jti" to UUID.randomUUID().toString()),
        )
    }

    private fun createToken(
        email: String,
        userId: Long,
        type: String,
        expiration: Long,
        additionalClaims: Map<String, Any>,
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        val builder =
            Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiryDate)

        additionalClaims.forEach { (key, value) ->
            builder.claim(key, value)
        }

        return builder.signWith(key).compact()
    }
}
