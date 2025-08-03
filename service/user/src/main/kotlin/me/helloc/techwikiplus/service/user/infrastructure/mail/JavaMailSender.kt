package me.helloc.techwikiplus.service.user.infrastructure.mail

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.service.port.MailSender
import org.springframework.stereotype.Component

@Component
class JavaMailSender : MailSender {
    override fun send(
        to: Email,
        subject: String,
        body: String,
    ) {
        TODO("Not yet implemented")
    }
}
