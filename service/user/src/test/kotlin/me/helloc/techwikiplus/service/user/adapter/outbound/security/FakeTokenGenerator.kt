package me.helloc.techwikiplus.service.user.adapter.outbound.security

import me.helloc.techwikiplus.service.user.application.port.outbound.TokenGenerator

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
