package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.value.Email

interface UserVerifyResendUseCase {
    fun execute(email: Email)
}
