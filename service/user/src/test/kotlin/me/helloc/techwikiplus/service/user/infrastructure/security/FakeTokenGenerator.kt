package me.helloc.techwikiplus.service.user.infrastructure.security

import me.helloc.techwikiplus.service.user.domain.service.port.TokenGenerator

class FakeTokenGenerator : TokenGenerator {
    var generateAccessTokenCalled = false
    var generateRefreshTokenCalled = false
    var lastUserId: String? = null

    override fun generateAccessToken(userId: String): String {
        generateAccessTokenCalled = true
        lastUserId = userId
        return "fake-access-token-$userId"
    }

    override fun generateRefreshToken(userId: String): String {
        generateRefreshTokenCalled = true
        lastUserId = userId
        return "fake-refresh-token-$userId"
    }
}
