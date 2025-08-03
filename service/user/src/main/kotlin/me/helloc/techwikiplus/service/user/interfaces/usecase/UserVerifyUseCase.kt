package me.helloc.techwikiplus.service.user.interfaces.usecase

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode

interface UserVerifyUseCase {
    fun execute(command: Command)

    data class Command(
        val email: Email,
        val code: VerificationCode,
    )
}
