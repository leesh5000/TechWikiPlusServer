package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.RegistrationCode

interface UserVerifyUseCase {
    fun execute(
        email: Email,
        code: RegistrationCode,
    )
}
