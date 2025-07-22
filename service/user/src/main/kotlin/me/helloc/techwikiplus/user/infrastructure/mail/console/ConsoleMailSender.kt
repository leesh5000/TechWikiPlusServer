package me.helloc.techwikiplus.user.infrastructure.mail.console

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.MailSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

class ConsoleMailSender : MailSender {
    @Value("\${spring.mail.username}")
    private lateinit var from: String
    private val logger = LoggerFactory.getLogger(ConsoleMailSender::class.java)
    private val sentEmails = mutableListOf<SentEmail>()

    data class SentEmail(
        val to: String,
        val subject: String,
        val body: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    override fun sendVerificationEmail(email: String): VerificationCode {
        val code = VerificationCode.generate()

        logger.info(
            """
            ===== FAKE EMAIL SENT =====
            From: $from
            To: $email
            Subject: TechWiki+ 이메일 인증
            Verification Code: ${code.value}
            ===========================
            """.trimIndent(),
        )

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
        logger.info(
            """
            ===== FAKE EMAIL SENT =====
            From: $from
            To: $email
            Subject: TechWiki+ 비밀번호 재설정
            Reset Code: $code
            ===========================
            """.trimIndent(),
        )

        sentEmails.add(
            SentEmail(
                to = email,
                subject = "TechWiki+ 비밀번호 재설정",
                body = "Reset Code: $code",
            ),
        )
    }

    // 테스트용 메서드
    fun getSentEmails(): List<SentEmail> = sentEmails.toList()

    fun getLastSentEmail(): SentEmail? = sentEmails.lastOrNull()

    fun clearSentEmails() = sentEmails.clear()

    fun getEmailsSentTo(email: String): List<SentEmail> = sentEmails.filter { it.to == email }
}
