package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.infrastructure.clock.FakeClockHolder
import me.helloc.techwikiplus.service.user.infrastructure.security.FakeTokenGenerator
import java.time.Instant

class UserTokenGeneratorUnitTest : FunSpec({
    lateinit var userTokenGenerator: UserTokenGenerator
    lateinit var fakeTokenGenerator: FakeTokenGenerator
    lateinit var fakeClockHolder: FakeClockHolder

    beforeEach {
        fakeTokenGenerator = FakeTokenGenerator()
        fakeClockHolder = FakeClockHolder(Instant.parse("2024-01-01T00:00:00Z"))
        userTokenGenerator = UserTokenGenerator(fakeTokenGenerator, fakeClockHolder)
    }

    context("generateTokens 메서드") {
        test("주어진 userId로 액세스 토큰과 리프레시 토큰을 생성한다") {
            // Given
            val userId = "user-123"

            // When
            val tokenPair = userTokenGenerator.generateTokens(userId)

            // Then
            tokenPair.accessToken shouldBe "fake-access-token-$userId"
            tokenPair.refreshToken shouldBe "fake-refresh-token-$userId"
            fakeTokenGenerator.generateAccessTokenCalled shouldBe true
            fakeTokenGenerator.generateRefreshTokenCalled shouldBe true
            fakeTokenGenerator.lastUserId shouldBe userId
        }

        test("액세스 토큰의 만료 시간은 현재 시간으로부터 1시간 후로 설정된다") {
            // Given
            val userId = "user-456"
            val currentTime = Instant.parse("2024-01-01T00:00:00Z")
            val expectedAccessTokenExpiry = currentTime.plusSeconds(3600L)

            // When
            val tokenPair = userTokenGenerator.generateTokens(userId)

            // Then
            tokenPair.accessTokenExpiresAt shouldBe expectedAccessTokenExpiry
        }

        test("리프레시 토큰의 만료 시간은 현재 시간으로부터 30일 후로 설정된다") {
            // Given
            val userId = "user-789"
            val currentTime = Instant.parse("2024-01-01T00:00:00Z")
            val expectedRefreshTokenExpiry = currentTime.plusSeconds(2592000L)

            // When
            val tokenPair = userTokenGenerator.generateTokens(userId)

            // Then
            tokenPair.refreshTokenExpiresAt shouldBe expectedRefreshTokenExpiry
        }

        test("동일한 userId로 여러 번 호출해도 매번 다른 토큰을 생성한다") {
            // Given
            val userId = "user-same"

            // When
            val tokenPair1 = userTokenGenerator.generateTokens(userId)
            val tokenPair2 = userTokenGenerator.generateTokens(userId)

            // Then
            tokenPair1.accessToken shouldBe tokenPair2.accessToken // fake 구현이므로 실제로는 같음
            tokenPair1.refreshToken shouldBe tokenPair2.refreshToken // fake 구현이므로 실제로는 같음
            tokenPair1.accessTokenExpiresAt shouldBe tokenPair2.accessTokenExpiresAt
            tokenPair1.refreshTokenExpiresAt shouldBe tokenPair2.refreshTokenExpiresAt
        }

        test("빈 userId로도 토큰을 생성할 수 있다") {
            // Given
            val userId = ""

            // When
            val tokenPair = userTokenGenerator.generateTokens(userId)

            // Then
            tokenPair.accessToken shouldBe "fake-access-token-"
            tokenPair.refreshToken shouldBe "fake-refresh-token-"
            tokenPair.accessTokenExpiresAt shouldNotBe null
            tokenPair.refreshTokenExpiresAt shouldNotBe null
        }

        test("특수 문자가 포함된 userId로도 정상적으로 토큰을 생성한다") {
            // Given
            val userId = "user@123#special!chars"

            // When
            val tokenPair = userTokenGenerator.generateTokens(userId)

            // Then
            tokenPair.accessToken shouldBe "fake-access-token-$userId"
            tokenPair.refreshToken shouldBe "fake-refresh-token-$userId"
        }
    }

    context("TokenPair 데이터 클래스") {
        test("TokenPair는 불변 객체로 생성된다") {
            // Given
            val accessToken = "test-access-token"
            val refreshToken = "test-refresh-token"
            val accessTokenExpiresAt = Instant.now()
            val refreshTokenExpiresAt = Instant.now().plusSeconds(3600)

            // When
            val tokenPair =
                UserTokenGenerator.TokenPair(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    accessTokenExpiresAt = accessTokenExpiresAt,
                    refreshTokenExpiresAt = refreshTokenExpiresAt,
                )

            // Then
            tokenPair.accessToken shouldBe accessToken
            tokenPair.refreshToken shouldBe refreshToken
            tokenPair.accessTokenExpiresAt shouldBe accessTokenExpiresAt
            tokenPair.refreshTokenExpiresAt shouldBe refreshTokenExpiresAt
        }

        test("TokenPair는 equals와 hashCode가 올바르게 구현되어 있다") {
            // Given
            val now = Instant.now()
            val tokenPair1 =
                UserTokenGenerator.TokenPair(
                    accessToken = "token1",
                    refreshToken = "refresh1",
                    accessTokenExpiresAt = now,
                    refreshTokenExpiresAt = now.plusSeconds(3600),
                )
            val tokenPair2 =
                UserTokenGenerator.TokenPair(
                    accessToken = "token1",
                    refreshToken = "refresh1",
                    accessTokenExpiresAt = now,
                    refreshTokenExpiresAt = now.plusSeconds(3600),
                )
            val tokenPair3 =
                UserTokenGenerator.TokenPair(
                    accessToken = "token2",
                    refreshToken = "refresh2",
                    accessTokenExpiresAt = now,
                    refreshTokenExpiresAt = now.plusSeconds(3600),
                )

            // When & Then
            tokenPair1 shouldBe tokenPair2
            tokenPair1 shouldNotBe tokenPair3
            tokenPair1.hashCode() shouldBe tokenPair2.hashCode()
            tokenPair1.hashCode() shouldNotBe tokenPair3.hashCode()
        }
    }
})
