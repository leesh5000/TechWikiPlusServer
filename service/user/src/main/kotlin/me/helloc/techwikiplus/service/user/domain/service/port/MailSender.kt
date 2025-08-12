package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.MailContent

interface MailSender {
    fun send(
        to: Email,
        content: MailContent,
    )
}
