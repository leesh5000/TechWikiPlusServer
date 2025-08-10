package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.FakeTokenManager

class UserTokenServiceTest : FunSpec({

    lateinit var userTokenService: UserTokenService
    lateinit var fakeTokenManager: FakeTokenManager

    beforeEach {
        fakeTokenManager = FakeTokenManager()
        userTokenService = UserTokenService(tokenManager = fakeTokenManager)
    }

    afterEach {
        fakeTokenManager.reset()
    }

    context("generateTokens 메서드는") {
        test("사용자 ID를 받아 액세스 토큰과 리프레시 토큰 쌍을 생성한다") {
            // given
            val userId = UserId(1000001L)

            // when
            val tokenPair = userTokenService.generateTokens(userId)

            // then
            tokenPair shouldNotBe null
            tokenPair.accessToken shouldNotBe null
            tokenPair.refreshToken shouldNotBe null
            tokenPair.accessToken.userId shouldBe userId
            tokenPair.refreshToken.userId shouldBe userId
        }

        test("생성된 액세스 토큰과 리프레시 토큰은 서로 다른 토큰이다") {
            // given
            val userId = UserId(1000001L)

            // when
            val tokenPair = userTokenService.generateTokens(userId)

            // then
            tokenPair.accessToken.token shouldNotBe tokenPair.refreshToken.token
        }

        test("액세스 토큰은 'fake-access-token'으로 시작한다") {
            // given
            val userId = UserId(1000001L)

            // when
            val tokenPair = userTokenService.generateTokens(userId)

            // then
            tokenPair.accessToken.token shouldStartWith "fake-access-token"
        }

        test("리프레시 토큰은 'fake-refresh-token'으로 시작한다") {
            // given
            val userId = UserId(1000001L)

            // when
            val tokenPair = userTokenService.generateTokens(userId)

            // then
            tokenPair.refreshToken.token shouldStartWith "fake-refresh-token"
        }

        test("액세스 토큰의 만료 시간은 리프레시 토큰보다 짧다") {
            // given
            val userId = UserId(1000001L)

            // when
            val tokenPair = userTokenService.generateTokens(userId)

            // then
            val accessExpiresAt = tokenPair.accessToken.expiresAt
            val refreshExpiresAt = tokenPair.refreshToken.expiresAt
            (refreshExpiresAt > accessExpiresAt) shouldBe true
        }

        test("동일한 사용자 ID로 여러 번 호출하면 매번 다른 토큰을 생성한다") {
            // given
            val userId = UserId(1000001L)

            // when
            val tokenPair1 = userTokenService.generateTokens(userId)
            val tokenPair2 = userTokenService.generateTokens(userId)

            // then
            tokenPair1.accessToken.token shouldNotBe tokenPair2.accessToken.token
            tokenPair1.refreshToken.token shouldNotBe tokenPair2.refreshToken.token

            tokenPair1.accessToken.token shouldContain "-1"
            tokenPair1.refreshToken.token shouldContain "-2"
            tokenPair2.accessToken.token shouldContain "-3"
            tokenPair2.refreshToken.token shouldContain "-4"
        }

        test("서로 다른 사용자 ID로 토큰을 생성할 수 있다") {
            // given
            val userId1 = UserId(1000001L)
            val userId2 = UserId(1000002L)

            // when
            val tokenPair1 = userTokenService.generateTokens(userId1)
            val tokenPair2 = userTokenService.generateTokens(userId2)

            // then
            tokenPair1.accessToken.userId shouldBe userId1
            tokenPair1.refreshToken.userId shouldBe userId1
            tokenPair2.accessToken.userId shouldBe userId2
            tokenPair2.refreshToken.userId shouldBe userId2

            tokenPair1.accessToken.token shouldNotBe tokenPair2.accessToken.token
            tokenPair1.refreshToken.token shouldNotBe tokenPair2.refreshToken.token
        }

        test("생성된 토큰은 TokenManager를 통해 검증 가능하다") {
            // given
            val userId = UserId(1000001L)

            // when
            val tokenPair = userTokenService.generateTokens(userId)

            // then
            val validatedUserId = fakeTokenManager.validateAccessToken(tokenPair.accessToken.token)
            validatedUserId shouldBe userId

            val validatedRefreshUserId =
                fakeTokenManager.validateRefreshToken(
                    userId,
                    tokenPair.refreshToken.token,
                )
            validatedRefreshUserId shouldBe userId
        }

        test("짧은 사용자 ID로도 토큰을 생성할 수 있다") {
            // given
            val userId = UserId(1L)

            // when
            val tokenPair = userTokenService.generateTokens(userId)

            // then
            tokenPair.accessToken.userId shouldBe userId
            tokenPair.refreshToken.userId shouldBe userId
        }

        test("최대 길이의 사용자 ID로도 토큰을 생성할 수 있다") {
            // given
            val longId = "a".repeat(64)
            val userId = UserId(longId)

            // when
            val tokenPair = userTokenService.generateTokens(userId)

            // then
            tokenPair.accessToken.userId shouldBe userId
            tokenPair.refreshToken.userId shouldBe userId
            tokenPair.accessToken.userId.value shouldBe longId
        }

        test("연속적으로 많은 토큰을 생성해도 각각 고유하다") {
            // given
            val userId = UserId(1000001L)
            val tokenPairs = mutableListOf<UserTokenService.TokenPair>()

            // when
            repeat(100) {
                tokenPairs.add(userTokenService.generateTokens(userId))
            }

            // then
            val accessTokens = tokenPairs.map { it.accessToken.token }.toSet()
            val refreshTokens = tokenPairs.map { it.refreshToken.token }.toSet()

            accessTokens.size shouldBe 100
            refreshTokens.size shouldBe 100

            // 액세스 토큰과 리프레시 토큰 간에도 중복이 없어야 함
            accessTokens.intersect(refreshTokens).isEmpty() shouldBe true
        }
    }

    context("TokenPair 데이터 클래스는") {
        test("동일한 토큰 쌍을 비교하면 같다") {
            // given
            val userId = UserId(1000001L)
            val tokenPair1 = userTokenService.generateTokens(userId)

            // when
            val tokenPair2 =
                UserTokenService.TokenPair(
                    accessToken = tokenPair1.accessToken,
                    refreshToken = tokenPair1.refreshToken,
                )

            // then
            tokenPair1 shouldBe tokenPair2
        }

        test("다른 토큰 쌍을 비교하면 다르다") {
            // given
            val userId = UserId(1000001L)

            // when
            val tokenPair1 = userTokenService.generateTokens(userId)
            val tokenPair2 = userTokenService.generateTokens(userId)

            // then
            tokenPair1 shouldNotBe tokenPair2
        }

        test("copy 메서드를 사용하여 일부 필드만 변경할 수 있다") {
            // given
            val userId1 = UserId(1000001L)
            val userId2 = UserId(1000002L)
            val tokenPair1 = userTokenService.generateTokens(userId1)
            val newAccessToken = fakeTokenManager.generateAccessToken(userId2)

            // when
            val tokenPair2 = tokenPair1.copy(accessToken = newAccessToken)

            // then
            tokenPair2.accessToken shouldBe newAccessToken
            tokenPair2.refreshToken shouldBe tokenPair1.refreshToken
            tokenPair2 shouldNotBe tokenPair1
        }
    }
})
