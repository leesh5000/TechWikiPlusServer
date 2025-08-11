package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.UserToken
import me.helloc.techwikiplus.service.user.domain.model.value.UserId

interface TokenManager {
    fun generateAccessToken(userId: UserId): UserToken

    fun generateRefreshToken(userId: UserId): UserToken

    fun validateRefreshToken(
        userId: UserId,
        refreshToken: String,
    ): UserId

    fun validateAccessToken(token: String): UserId
}
