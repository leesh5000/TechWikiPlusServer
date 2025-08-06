package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode

interface UserVerifyUseCase {
    fun execute(
        email: Email,
        code: RegistrationCode,
    )
}
