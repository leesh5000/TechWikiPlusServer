package me.helloc.techwikiplus.service.user.domain.service.port

interface MailSender {
    fun send(
        to: String,
        subject: String,
        body: String,
    )
}
