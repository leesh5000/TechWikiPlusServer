package me.helloc.techwikiplus.user.infrastructure.mail.fake

import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.io.InputStream
import java.util.Properties

class FakeJavaMailSender : JavaMailSender {
    private var shouldThrowOnSend = false
    private var shouldThrowOnCreateMessage = false
    private var exceptionToThrow: Exception? = null
    private val sentMessages = mutableListOf<MimeMessage>()

    fun setShouldThrowOnSend(exception: Exception) {
        shouldThrowOnSend = true
        exceptionToThrow = exception
    }

    fun setShouldThrowOnCreateMessage(exception: Exception) {
        shouldThrowOnCreateMessage = true
        exceptionToThrow = exception
    }

    fun reset() {
        shouldThrowOnSend = false
        shouldThrowOnCreateMessage = false
        exceptionToThrow = null
        sentMessages.clear()
    }

    override fun createMimeMessage(): MimeMessage {
        if (shouldThrowOnCreateMessage) {
            throw exceptionToThrow ?: RuntimeException("Failed to create message")
        }
        return FakeMimeMessage()
    }

    override fun createMimeMessage(contentStream: InputStream): MimeMessage {
        return FakeMimeMessage()
    }

    override fun send(mimeMessage: MimeMessage) {
        if (shouldThrowOnSend) {
            throw exceptionToThrow ?: MailSendException("Failed to send message")
        }
        sentMessages.add(mimeMessage)
    }

    override fun send(vararg mimeMessages: MimeMessage) {
        mimeMessages.forEach { send(it) }
    }

    override fun send(simpleMessage: SimpleMailMessage) {
        // Not used in our tests
    }

    override fun send(vararg simpleMessages: SimpleMailMessage) {
        // Not used in our tests
    }

    private class FakeMimeMessage : MimeMessage(Session.getInstance(Properties()))
}
