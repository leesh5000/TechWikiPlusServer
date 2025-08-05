package me.helloc.techwikiplus.service.user.application.port.inbound

import java.time.Instant

interface UserLoginRefreshUseCase {
    fun execute(command: Command): Result

    data class Command(
        val refreshToken: String,
    )

    data class Result(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val accessTokenExpiresAt: Instant,
        val refreshTokenExpiresAt: Instant,
    )
}
