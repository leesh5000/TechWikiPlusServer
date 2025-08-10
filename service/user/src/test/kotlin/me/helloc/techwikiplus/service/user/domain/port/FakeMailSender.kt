package me.helloc.techwikiplus.service.user.domain.port

import me.helloc.techwikiplus.service.user.domain.model.MailContent
import me.helloc.techwikiplus.service.user.domain.model.value.Email

open class FakeMailSender : MailSender {
    data class SentMail(
        val to: Email,
        val content: MailContent,
    )

    private val sentMails = mutableListOf<SentMail>()

    override fun send(
        to: Email,
        content: MailContent,
    ) {
        sentMails.add(SentMail(to, content))
    }

    fun getSentMails(): List<SentMail> {
        return sentMails.toList()
    }

    fun getLastSentMail(): SentMail? {
        return sentMails.lastOrNull()
    }

    fun getSentMailCount(): Int {
        return sentMails.size
    }

    fun wasSentTo(email: Email): Boolean {
        return sentMails.any { it.to == email }
    }

    fun clear() {
        sentMails.clear()
    }
}
