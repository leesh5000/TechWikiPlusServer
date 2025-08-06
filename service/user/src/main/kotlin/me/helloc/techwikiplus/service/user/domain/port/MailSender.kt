package me.helloc.techwikiplus.service.user.domain.port

import me.helloc.techwikiplus.service.user.domain.model.MailContent
import me.helloc.techwikiplus.service.user.domain.model.value.Email

interface MailSender {
    fun send(
        to: Email,
        content: MailContent,
    )
}
