package me.helloc.techwikiplus.service.user.infrastructure.token.jwt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class JwtTokenGeneratorTest : FunSpec({

    test("액세스 토큰을 생성해야 한다") {
        // given
        val secret = "test-secret-key-must-be-at-least-256-bits-long"
        val generator = JwtTokenGenerator(secret)
        val userId = "12345"

        // when
        val token = generator.generateAccessToken(userId)

        // then
        token shouldNotBe null
        token.isNotBlank() shouldBe true
    }

    test("리프레시 토큰을 생성해야 한다") {
        // given
        val secret = "test-secret-key-must-be-at-least-256-bits-long"
        val generator = JwtTokenGenerator(secret)
        val userId = "12345"

        // when
        val token = generator.generateRefreshToken(userId)

        // then
        token shouldNotBe null
        token.isNotBlank() shouldBe true
    }

    test("생성된 토큰은 유효한 JWT 형식이어야 한다") {
        // given
        val secret = "test-secret-key-must-be-at-least-256-bits-long"
        val generator = JwtTokenGenerator(secret)
        val userId = "12345"

        // when
        val accessToken = generator.generateAccessToken(userId)
        val refreshToken = generator.generateRefreshToken(userId)

        // then
        // JWT format: header.payload.signature
        accessToken.split(".").size shouldBe 3
        refreshToken.split(".").size shouldBe 3
    }
})
