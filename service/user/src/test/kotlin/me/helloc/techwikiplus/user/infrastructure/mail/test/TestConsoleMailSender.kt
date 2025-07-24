package me.helloc.techwikiplus.user.infrastructure.mail.test

import me.helloc.techwikiplus.user.infrastructure.mail.console.ConsoleMailSender

class TestConsoleMailSender : ConsoleMailSender() {
    private val sentEmails = mutableListOf<SentEmail>()

    data class SentEmail(
        val to: String,
        val subject: String,
        val body: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    override fun sendVerificationEmail(email: String): me.helloc.techwikiplus.user.domain.VerificationCode {
        val code = super.sendVerificationEmail(email)
        sentEmails.add(
            SentEmail(
                to = email,
                subject = "TechWiki+ 이메일 인증",
                body = "Verification Code: ${code.value}",
            ),
        )
        return code
    }

    override fun sendPasswordResetEmail(
        email: String,
        code: String,
    ) {
        super.sendPasswordResetEmail(email, code)
        sentEmails.add(
            SentEmail(
                to = email,
                subject = "TechWiki+ 비밀번호 재설정",
                body = "Reset Code: $code",
            ),
        )
    }

    fun getSentEmails(): List<SentEmail> = sentEmails.toList()

    fun getLastSentEmail(): SentEmail? = sentEmails.lastOrNull()

    fun clearSentEmails() = sentEmails.clear()

    fun getEmailsSentTo(email: String): List<SentEmail> = sentEmails.filter { it.to == email }
}
