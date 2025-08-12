package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserToken

interface UserLoginRefreshUseCase {
    fun execute(
        userId: UserId,
        refreshToken: String,
    ): Result

    data class Result(
        val accessToken: UserToken,
        val refreshToken: UserToken,
        val userId: UserId,
    )
}
