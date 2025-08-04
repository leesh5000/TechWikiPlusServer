package me.helloc.techwikiplus.service.user.interfaces.usecase

import java.time.Instant

interface UserLoginUseCase {
    fun execute(command: Command): Result

    data class Command(
        val email: String,
        val password: String,
    )

    data class Result(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val accessTokenExpiresAt: Instant,
        val refreshTokenExpiresAt: Instant,
    )
}
