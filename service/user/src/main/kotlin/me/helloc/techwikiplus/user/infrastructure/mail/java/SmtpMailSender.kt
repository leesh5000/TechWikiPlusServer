package me.helloc.techwikiplus.user.infrastructure.mail.java

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.port.outbound.EmailTemplateGenerator
import me.helloc.techwikiplus.user.domain.port.outbound.EmailTemplateGenerator.EmailTemplateDetails
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.infrastructure.exception.MailDeliveryException
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper

class SmtpMailSender(
    private val mailSender: JavaMailSender,
    private val emailTemplateGenerator: EmailTemplateGenerator,
    private val from: String,
) : MailSender {
    private val log = LoggerFactory.getLogger(SmtpMailSender::class.java)

    override fun sendVerificationEmail(email: String): VerificationCode {
        val code = VerificationCode.generate()
        val emailTemplate = emailTemplateGenerator.generateVerificationEmail(code.value)
        sendEmail(email, emailTemplate)
        return code
    }

    override fun sendPasswordResetEmail(
        email: String,
        code: String,
    ) {
        val emailTemplate = emailTemplateGenerator.generatePasswordResetEmail(code)
        sendEmail(email, emailTemplate)
    }

    private fun sendEmail(
        email: String,
        emailTemplateDetails: EmailTemplateDetails,
    ) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            helper.setFrom(from)
            helper.setTo(email)
            helper.setSubject(emailTemplateDetails.subject)
            helper.setText(emailTemplateDetails.body, true) // true for HTML
            mailSender.send(message)
        } catch (e: Exception) {
            log.error("Failed to send email to $email", e)
            throw MailDeliveryException(email, e)
        }
    }
}
