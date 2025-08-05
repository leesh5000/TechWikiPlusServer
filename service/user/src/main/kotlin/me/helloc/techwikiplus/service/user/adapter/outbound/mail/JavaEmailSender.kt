package me.helloc.techwikiplus.service.user.adapter.outbound.mail

import me.helloc.techwikiplus.service.user.application.port.outbound.EmailSender
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import org.springframework.stereotype.Component

@Component
class JavaEmailSender: EmailSender {
    override fun send(
        to: Email,
        content: Regi
    ) {
        TODO("Not yet implemented")
    }
}