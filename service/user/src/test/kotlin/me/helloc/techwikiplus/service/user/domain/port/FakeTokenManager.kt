package me.helloc.techwikiplus.service.user.domain.port

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.UserToken
import me.helloc.techwikiplus.service.user.domain.model.value.UserId

class FakeTokenManager : TokenManager {
    private val refreshTokens = mutableMapOf<String, UserId>()
    private val accessTokens = mutableMapOf<String, UserId>()
    private var tokenCounter = 0

    override fun generateAccessToken(userId: UserId): UserToken {
        tokenCounter++
        val token = "fake-access-token-$tokenCounter"
        accessTokens[token] = userId
        return UserToken(
            userId = userId,
            token = token,
            // 1시간
            expiresAt = System.currentTimeMillis() + 3600000,
        )
    }

    override fun generateRefreshToken(userId: UserId): UserToken {
        tokenCounter++
        val token = "fake-refresh-token-$tokenCounter"
        refreshTokens[token] = userId
        return UserToken(
            userId = userId,
            token = token,
            // 7일
            expiresAt = System.currentTimeMillis() + 86400000 * 7,
        )
    }

    override fun validateRefreshToken(
        userId: UserId,
        refreshToken: String,
    ): UserId {
        val storedUserId =
            refreshTokens[refreshToken]
                ?: throw DomainException(ErrorCode.INVALID_TOKEN)

        if (storedUserId != userId) {
            throw DomainException(ErrorCode.INVALID_TOKEN)
        }

        return storedUserId
    }

    override fun validateAccessToken(token: String): UserId {
        return accessTokens[token]
            ?: throw DomainException(ErrorCode.INVALID_TOKEN)
    }

    fun addRefreshToken(
        token: String,
        userId: UserId,
    ) {
        refreshTokens[token] = userId
    }

    fun addAccessToken(
        token: String,
        userId: UserId,
    ) {
        accessTokens[token] = userId
    }

    fun clearTokens() {
        refreshTokens.clear()
        accessTokens.clear()
        tokenCounter = 0
    }

    fun reset() {
        clearTokens()
    }
}
