package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.UserToken
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.value.UserId

interface UserLoginUseCase {
    fun execute(
        email: Email,
        password: RawPassword,
    ): Result

    data class Result(
        val accessToken: UserToken,
        val refreshToken: UserToken,
        val userId: UserId,
    )
}
