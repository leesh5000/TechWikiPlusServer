package me.helloc.techwikiplus.service.user.adapter.outbound.messaging

import me.helloc.techwikiplus.service.user.application.port.outbound.MailSender
import me.helloc.techwikiplus.service.user.domain.model.value.Email

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

    fun hasMailBeenSentTo(email: Email): Boolean {
        return sentMails.any { it.to == email.value }
    }

    fun getMailsSentTo(email: String): List<SentMail> {
        return sentMails.filter { it.to == email }
    }
}
