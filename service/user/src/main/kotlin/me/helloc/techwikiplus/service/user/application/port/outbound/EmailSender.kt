package me.helloc.techwikiplus.service.user.application.port.outbound

import me.helloc.techwikiplus.service.user.domain.model.MailContent
import me.helloc.techwikiplus.service.user.domain.model.value.Email

interface EmailSender {

    fun send(to: Email, content: MailContent)
}