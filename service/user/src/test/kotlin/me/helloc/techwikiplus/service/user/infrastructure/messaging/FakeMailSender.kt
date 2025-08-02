package me.helloc.techwikiplus.service.user.infrastructure.messaging

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.service.port.MailSender

class FakeMailSender : MailSender {
    data class SentMail(
        val to: String,
        val subject: String,
        val body: String,
    )

    private val sentMails = mutableListOf<SentMail>()

    override fun send(
        to: Email,
        subject: String,
        body: String,
    ) {
        sentMails.add(SentMail(to.value, subject, body))
    }

    fun getSentMails(): List<SentMail> = sentMails.toList()

    fun getLastSentMail(): SentMail? = sentMails.lastOrNull()

    fun clear() {
        sentMails.clear()
    }

    fun hasMailBeenSentTo(email: String): Boolean {
        return sentMails.any { it.to == email }
    }

    fun getMailsSentTo(email: String): List<SentMail> {
        return sentMails.filter { it.to == email }
    }
}
