package me.helloc.techwikiplus.service.user.application.port.inbound

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode

interface UserVerifyUseCase {
    fun execute(command: Command)

    data class Command(
        val email: Email,
        val code: RegistrationCode,
    )
}
