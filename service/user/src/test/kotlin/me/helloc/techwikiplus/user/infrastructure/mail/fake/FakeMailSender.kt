package me.helloc.techwikiplus.user.infrastructure.mail.fake

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.MailSender

class FakeMailSender : MailSender {
    private val sentEmails = mutableListOf<SentEmail>()

    data class SentEmail(
        val email: String,
        val type: EmailType,
        val code: String,
    )

    enum class EmailType {
        VERIFICATION,
        PASSWORD_RESET,
    }

    override fun sendVerificationEmail(email: String): VerificationCode {
        val code = VerificationCode.generate()
        sentEmails.add(SentEmail(email, EmailType.VERIFICATION, code.value))
        return code
    }

    override fun sendPasswordResetEmail(
        email: String,
        code: String,
    ) {
        sentEmails.add(SentEmail(email, EmailType.PASSWORD_RESET, code))
    }

    fun getSentEmails(): List<SentEmail> = sentEmails.toList()

    fun getLastSentEmail(): SentEmail? = sentEmails.lastOrNull()

    fun clear() {
        sentEmails.clear()
    }

    fun wasEmailSentTo(email: String): Boolean {
        return sentEmails.any { it.email == email }
    }
}
