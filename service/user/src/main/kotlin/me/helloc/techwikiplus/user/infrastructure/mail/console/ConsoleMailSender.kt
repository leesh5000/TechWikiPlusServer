package me.helloc.techwikiplus.user.infrastructure.mail.console

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.MailSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

open class ConsoleMailSender : MailSender {
    @Value("\${spring.mail.username}")
    private lateinit var from: String
    private val logger = LoggerFactory.getLogger(ConsoleMailSender::class.java)

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
    }
}
