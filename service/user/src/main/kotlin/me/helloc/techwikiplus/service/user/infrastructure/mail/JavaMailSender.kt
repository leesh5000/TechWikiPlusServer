package me.helloc.techwikiplus.service.user.infrastructure.mail

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.MailContent
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.service.port.MailSender
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.mail.MailProperties
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.springframework.mail.javamail.JavaMailSender as SpringJavaMailSender

@Component
class JavaMailSender(
    private val springMailSender: SpringJavaMailSender,
    private val mailProperties: MailProperties,
) : MailSender {
    private val logger = LoggerFactory.getLogger(JavaMailSender::class.java)

    override fun send(
        to: Email,
        content: MailContent,
    ) {
        require(content.subject.isNotEmpty()) { "Subject cannot be empty" }
        require(content.body.isNotEmpty()) { "Body cannot be empty" }

        try {
            if (isHtmlContent(content.body)) {
                sendHtmlEmail(to, content.subject, content.body)
            } else {
                sendPlainTextEmail(to, content.subject, content.body)
            }
            logger.info("Email sent successfully to: ${to.value}")
        } catch (e: Exception) {
            logger.error("Failed to send email to: ${to.value}", e)
            throw DomainException(
                errorCode = ErrorCode.NOTIFICATION_FAILED,
                params = arrayOf(to.value),
                cause = e,
            )
        }
    }

    private fun sendPlainTextEmail(
        to: Email,
        subject: String,
        body: String,
    ) {
        val message =
            SimpleMailMessage().apply {
                setTo(to.value)
                setSubject(subject)
                text = body
                from = mailProperties.username
            }
        springMailSender.send(message)
    }

    private fun sendHtmlEmail(
        to: Email,
        subject: String,
        body: String,
    ) {
        val mimeMessage = springMailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

        helper.setTo(to.value)
        helper.setSubject(subject)
        helper.setText(body, true)
        helper.setFrom(mailProperties.username)

        springMailSender.send(mimeMessage)
    }

    private fun isHtmlContent(content: String): Boolean {
        return content.contains("<html", ignoreCase = true) ||
            content.contains("<body", ignoreCase = true) ||
            content.contains("<div", ignoreCase = true) ||
            content.contains("<p>", ignoreCase = true) ||
            content.contains("<br", ignoreCase = true)
    }
}
