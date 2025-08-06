package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword

interface UserSignUpUseCase {
    fun execute(
        email: Email,
        nickname: Nickname,
        password: RawPassword,
        confirmPassword: RawPassword,
    )
}
