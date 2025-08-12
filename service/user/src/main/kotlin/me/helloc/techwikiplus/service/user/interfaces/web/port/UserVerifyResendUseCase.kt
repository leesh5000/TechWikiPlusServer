package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.Email

interface UserVerifyResendUseCase {
    fun execute(email: Email)
}
