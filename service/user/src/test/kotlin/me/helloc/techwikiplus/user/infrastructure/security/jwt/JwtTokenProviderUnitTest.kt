package me.helloc.techwikiplus.user.infrastructure.security.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
import me.helloc.techwikiplus.user.infrastructure.security.JwtTokenProvider
import java.util.Date

class JwtTokenProviderUnitTest : FunSpec({

    val secret = "test-secret-key-for-jwt-token-provider-unit-test-2024"
    val accessTokenExpiration = 3600000L // 1시간
    val refreshTokenExpiration = 604800000L // 7일

    lateinit var jwtProperties: JwtProperties
    lateinit var tokenProvider: JwtTokenProvider

    beforeEach {
        jwtProperties =
            JwtProperties(
                secret = secret,
                accessTokenExpiration = accessTokenExpiration,
                refreshTokenExpiration = refreshTokenExpiration,
            )
        tokenProvider = JwtTokenProvider(jwtProperties)
    }

    context("Access Token 생성") {
        test("유효한 access token을 생성한다") {
            val email = "test@example.com"
            val userId = 12345L

            val token = tokenProvider.createAccessToken(email, userId)

            token shouldNotBe null
            token.split(".").size shouldBe 3 // JWT는 3개 부분으로 구성

            // 토큰 검증
            tokenProvider.validateToken(token) shouldBe true
            tokenProvider.getEmailFromToken(token) shouldBe email
            tokenProvider.getUserIdFromToken(token) shouldBe userId
            tokenProvider.getTokenType(token) shouldBe "access"
        }

        test("access token은 설정된 만료 시간을 가진다") {
            val email = "test@example.com"
            val userId = 12345L
            val beforeCreation = System.currentTimeMillis()

            val token = tokenProvider.createAccessToken(email, userId)

            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            val claims =
                Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            val issuedAt = claims.issuedAt.time
            val expiration = claims.expiration.time

            // 발급 시간 확인
            issuedAt shouldNotBe null
            (issuedAt >= beforeCreation - 1000) shouldBe true // 1초 여유를 둔 검증

            // 만료 시간 확인 (1시간 후)
            (expiration - issuedAt) shouldBe accessTokenExpiration
        }
    }

    context("Refresh Token 생성") {
        test("유효한 refresh token을 생성한다") {
            val email = "test@example.com"
            val userId = 67890L

            val token = tokenProvider.createRefreshToken(email, userId)

            token shouldNotBe null
            token.split(".").size shouldBe 3

            // 토큰 검증
            tokenProvider.validateToken(token) shouldBe true
            tokenProvider.getEmailFromToken(token) shouldBe email
            tokenProvider.getUserIdFromToken(token) shouldBe userId
            tokenProvider.getTokenType(token) shouldBe "refresh"
        }

        test("refresh token은 설정된 만료 시간을 가진다") {
            val email = "test@example.com"
            val userId = 67890L

            val token = tokenProvider.createRefreshToken(email, userId)

            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            val claims =
                Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            val issuedAt = claims.issuedAt.time
            val expiration = claims.expiration.time

            // 만료 시간 확인 (7일 후)
            (expiration - issuedAt) shouldBe refreshTokenExpiration
        }
    }

    context("토큰 검증") {
        test("유효한 토큰은 true를 반환한다") {
            val token = tokenProvider.createAccessToken("test@example.com", 123L)

            tokenProvider.validateToken(token) shouldBe true
        }

        test("만료된 토큰은 false를 반환한다") {
            // 만료된 토큰 생성
            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            val expiredToken =
                Jwts.builder()
                    .subject("test@example.com")
                    .claim("userId", 123L)
                    .claim("type", "access")
                    .issuedAt(Date(System.currentTimeMillis() - 7200000)) // 2시간 전
                    .expiration(Date(System.currentTimeMillis() - 3600000)) // 1시간 전 만료
                    .signWith(key)
                    .compact()

            tokenProvider.validateToken(expiredToken) shouldBe false
        }

        test("잘못된 서명의 토큰은 false를 반환한다") {
            // 다른 키로 서명된 토큰
            val wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-12345678901234567890".toByteArray())
            val invalidToken =
                Jwts.builder()
                    .subject("test@example.com")
                    .claim("userId", 123L)
                    .claim("type", "access")
                    .issuedAt(Date())
                    .expiration(Date(System.currentTimeMillis() + 3600000))
                    .signWith(wrongKey)
                    .compact()

            tokenProvider.validateToken(invalidToken) shouldBe false
        }

        test("형식이 잘못된 토큰은 false를 반환한다") {
            val malformedToken = "this.is.not.a.valid.jwt.token"

            tokenProvider.validateToken(malformedToken) shouldBe false
        }

        test("빈 토큰은 false를 반환한다") {
            tokenProvider.validateToken("") shouldBe false
        }
    }

    context("토큰에서 정보 추출") {
        test("토큰에서 이메일을 추출한다") {
            val email = "user@example.com"
            val token = tokenProvider.createAccessToken(email, 456L)

            tokenProvider.getEmailFromToken(token) shouldBe email
        }

        test("토큰에서 userId를 추출한다") {
            val userId = 789L
            val token = tokenProvider.createAccessToken("test@example.com", userId)

            tokenProvider.getUserIdFromToken(token) shouldBe userId
        }

        test("토큰에서 type을 추출한다") {
            val accessToken = tokenProvider.createAccessToken("test@example.com", 123L)
            val refreshToken = tokenProvider.createRefreshToken("test@example.com", 123L)

            tokenProvider.getTokenType(accessToken) shouldBe "access"
            tokenProvider.getTokenType(refreshToken) shouldBe "refresh"
        }

        test("만료된 토큰에서 정보 추출 시 예외가 발생한다") {
            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            val expiredToken =
                Jwts.builder()
                    .subject("test@example.com")
                    .claim("userId", 123L)
                    .claim("type", "access")
                    .issuedAt(Date(System.currentTimeMillis() - 7200000))
                    .expiration(Date(System.currentTimeMillis() - 3600000))
                    .signWith(key)
                    .compact()

            val exception =
                shouldThrow<ExpiredJwtException> {
                    tokenProvider.getEmailFromToken(expiredToken)
                }
            exception.shouldBeInstanceOf<ExpiredJwtException>()
        }

        test("잘못된 서명의 토큰에서 정보 추출 시 예외가 발생한다") {
            val wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-12345678901234567890".toByteArray())
            val invalidToken =
                Jwts.builder()
                    .subject("test@example.com")
                    .claim("userId", 123L)
                    .claim("type", "access")
                    .issuedAt(Date())
                    .expiration(Date(System.currentTimeMillis() + 3600000))
                    .signWith(wrongKey)
                    .compact()

            shouldThrow<SignatureException> {
                tokenProvider.getEmailFromToken(invalidToken)
            }
        }
    }

    context("토큰 생성 시 필수 claim 확인") {
        test("access token은 필수 claim을 포함한다") {
            val email = "test@example.com"
            val userId = 999L
            val token = tokenProvider.createAccessToken(email, userId)

            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            val claims =
                Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            claims.subject shouldBe email
            when (val userIdClaim = claims.get("userId")) {
                is Long -> userIdClaim shouldBe userId
                is Int -> userIdClaim.toLong() shouldBe userId
                else -> fail("userId claim should be a numeric type")
            }
            claims.get("type", String::class.java) shouldBe "access"
            claims.issuedAt shouldNotBe null
            claims.expiration shouldNotBe null
        }

        test("refresh token은 필수 claim을 포함한다") {
            val email = "test@example.com"
            val userId = 999L
            val token = tokenProvider.createRefreshToken(email, userId)

            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            val claims =
                Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            claims.subject shouldBe email
            when (val userIdClaim = claims.get("userId")) {
                is Long -> userIdClaim shouldBe userId
                is Int -> userIdClaim.toLong() shouldBe userId
                else -> fail("userId claim should be a numeric type")
            }
            claims.get("type", String::class.java) shouldBe "refresh"
            claims.issuedAt shouldNotBe null
            claims.expiration shouldNotBe null
        }
    }
})
