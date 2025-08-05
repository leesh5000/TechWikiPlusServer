package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.ExpiredTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenTypeException
import me.helloc.techwikiplus.service.user.domain.service.port.TokenValidator
import me.helloc.techwikiplus.service.user.infrastructure.token.FakeTokenValidator
import java.time.Instant

class UserTokenValidatorUnitTest : FunSpec(
    {
        context("validateRefreshTokenOrThrows") {
            test("유효한 refresh 토큰이 주어지면 TokenClaims를 반환한다") {
                // Given
                val fakeTokenValidator = FakeTokenValidator()
                val userTokenValidator = UserTokenValidator(fakeTokenValidator)
                val token = "valid-refresh-token"
                val userId = "user123"
                val expectedClaims =
                    TokenValidator.TokenClaims(
                        userId = userId,
                        tokenType = "refresh",
                        issuedAt = Instant.now().minusSeconds(3600),
                        expiresAt = Instant.now().plusSeconds(86400),
                    )
                fakeTokenValidator.addValidToken(token, expectedClaims)

                // When
                val actualClaims = userTokenValidator.validateRefreshTokenOrThrows(token)

                // Then
                actualClaims.userId shouldBe expectedClaims.userId
                actualClaims.tokenType shouldBe expectedClaims.tokenType
                actualClaims.issuedAt shouldBe expectedClaims.issuedAt
                actualClaims.expiresAt shouldBe expectedClaims.expiresAt
            }

            test("유효하지 않은 토큰이 주어지면 InvalidTokenException을 던진다") {
                // Given
                val fakeTokenValidator = FakeTokenValidator()
                val userTokenValidator = UserTokenValidator(fakeTokenValidator)
                val invalidToken = "invalid-token"

                // When & Then
                val exception =
                    shouldThrow<InvalidTokenException> {
                        userTokenValidator.validateRefreshTokenOrThrows(invalidToken)
                    }
                exception.message shouldBe "Invalid token format"
            }

            test("만료된 토큰이 주어지면 ExpiredTokenException을 던진다") {
                // Given
                val fakeTokenValidator = FakeTokenValidator()
                val userTokenValidator = UserTokenValidator(fakeTokenValidator)
                val expiredToken = "expired-token"
                val expiredClaims =
                    TokenValidator.TokenClaims(
                        userId = "user123",
                        tokenType = "refresh",
                        // 2 days ago
                        issuedAt = Instant.now().minusSeconds(172800),
                        // 1 day ago
                        expiresAt = Instant.now().minusSeconds(86400),
                    )
                fakeTokenValidator.addValidToken(expiredToken, expiredClaims)

                // When & Then
                shouldThrow<ExpiredTokenException> {
                    userTokenValidator.validateRefreshTokenOrThrows(expiredToken)
                }
            }

            test("refresh 타입이 아닌 토큰이 주어지면 InvalidTokenTypeException을 던진다") {
                // Given
                val fakeTokenValidator = FakeTokenValidator()
                val userTokenValidator = UserTokenValidator(fakeTokenValidator)
                val accessToken = "access-token"
                val accessTokenClaims =
                    TokenValidator.TokenClaims(
                        userId = "user123",
                        tokenType = "access",
                        issuedAt = Instant.now().minusSeconds(300),
                        expiresAt = Instant.now().plusSeconds(3600),
                    )
                fakeTokenValidator.addValidToken(accessToken, accessTokenClaims)

                // When & Then
                val exception =
                    shouldThrow<InvalidTokenTypeException> {
                        userTokenValidator.validateRefreshTokenOrThrows(accessToken)
                    }
                exception.message shouldBe "Invalid token type. Expected: refresh, but got: access"
            }

            test("FakeTokenValidator의 addValidRefreshToken 헬퍼 메서드를 사용하여 유효한 refresh 토큰을 검증한다") {
                // Given
                val fakeTokenValidator = FakeTokenValidator()
                val userTokenValidator = UserTokenValidator(fakeTokenValidator)
                val token = "refresh-token"
                val userId = "user456"
                fakeTokenValidator.addValidRefreshToken(token, userId)

                // When
                val claims = userTokenValidator.validateRefreshTokenOrThrows(token)

                // Then
                claims.userId shouldBe userId
                claims.tokenType shouldBe "refresh"
            }
        }
    },
)
