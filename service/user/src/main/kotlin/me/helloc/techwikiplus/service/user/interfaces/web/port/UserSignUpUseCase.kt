package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.RawPassword

interface UserSignUpUseCase {
    fun handle(
        email: Email,
        nickname: Nickname,
        password: RawPassword,
        confirmPassword: RawPassword,
    )
}
