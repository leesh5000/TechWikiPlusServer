package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserToken

interface TokenManager {
    fun generateAccessToken(userId: UserId): UserToken

    fun generateRefreshToken(userId: UserId): UserToken

    fun validateRefreshToken(
        userId: UserId,
        refreshToken: String,
    ): UserId

    fun validateAccessToken(token: String): UserId
}
