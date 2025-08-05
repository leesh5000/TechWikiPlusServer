package me.helloc.techwikiplus.service.user.application.port.outbound

import me.helloc.techwikiplus.service.user.domain.model.value.Email

interface MailSender {
    fun send(
        to: Email,
        subject: String,
        body: String,
    )
}
