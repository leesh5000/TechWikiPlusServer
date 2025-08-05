package me.helloc.techwikiplus.service.user.adapter.outbound.mail

import me.helloc.techwikiplus.service.user.application.port.outbound.MailSender
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.mail.MailProperties
import org.springframework.mail.MailException
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
        subject: String,
        body: String,
    ) {
        require(subject.isNotEmpty()) { "Subject cannot be empty" }
        require(body.isNotEmpty()) { "Body cannot be empty" }

        try {
            if (isHtmlContent(body)) {
                sendHtmlEmail(to, subject, body)
            } else {
                sendPlainTextEmail(to, subject, body)
            }
            logger.info("Email sent successfully to: ${to.value}")
        } catch (e: MailException) {
            logger.error("Failed to send email to: ${to.value}", e)
            throw MailSendingException(
                "Failed to send email to: ${to.value}",
                e,
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
