package me.helloc.techwikiplus.service.user.adapter.outbound.mail

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import org.springframework.boot.autoconfigure.mail.MailProperties
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender as SpringJavaMailSender

class JavaMailSenderUnitTest : DescribeSpec({

    // Fake implementation of Spring's JavaMailSender for testing
    class FakeSpringJavaMailSender : SpringJavaMailSender {
        var sentSimpleMessages = mutableListOf<SimpleMailMessage>()
        var sentMimeMessages = mutableListOf<MimeMessage>()
        var shouldThrowException: MailException? = null
        private val mimeMessage = MimeMessage(null as Session?)

        override fun send(simpleMessage: SimpleMailMessage) {
            shouldThrowException?.let { throw it }
            sentSimpleMessages.add(simpleMessage)
        }

        override fun send(vararg simpleMessages: SimpleMailMessage) {
            simpleMessages.forEach { send(it) }
        }

        override fun createMimeMessage(): MimeMessage = mimeMessage

        override fun createMimeMessage(contentStream: java.io.InputStream): MimeMessage = mimeMessage

        override fun send(mimeMessage: MimeMessage) {
            shouldThrowException?.let { throw it }
            sentMimeMessages.add(mimeMessage)
        }

        override fun send(vararg mimeMessages: MimeMessage) {
            mimeMessages.forEach { send(it) }
        }

        fun reset() {
            sentSimpleMessages.clear()
            sentMimeMessages.clear()
            shouldThrowException = null
        }

        fun getLastSimpleMessage() = sentSimpleMessages.lastOrNull()

        fun getLastMimeMessage() = sentMimeMessages.lastOrNull()
    }

    describe("JavaMailSender") {
        val fakeSpringMailSender = FakeSpringJavaMailSender()
        val mailProperties =
            MailProperties().apply {
                username = "sender@example.com"
            }
        val javaMailSender = JavaMailSender(fakeSpringMailSender, mailProperties)

        beforeEach {
            fakeSpringMailSender.reset()
        }

        describe("send 메서드") {
            context("유효한 이메일 정보가 주어졌을 때") {
                it("SimpleMailMessage를 사용하여 이메일을 전송해야 한다") {
                    // Given
                    val to = Email("test@example.com")
                    val subject = "Test Subject"
                    val body = "Test Body"

                    // When
                    javaMailSender.send(to, subject, body)

                    // Then
                    fakeSpringMailSender.sentSimpleMessages.size shouldBe 1
                    val sentMessage = fakeSpringMailSender.getLastSimpleMessage()!!
                    sentMessage.to?.get(0) shouldBe "test@example.com"
                    sentMessage.subject shouldBe "Test Subject"
                    sentMessage.text shouldBe "Test Body"
                    sentMessage.from shouldBe "sender@example.com"
                }
            }

            context("HTML 콘텐츠가 포함된 이메일을 전송할 때") {
                it("MimeMessage를 사용하여 HTML 이메일을 전송해야 한다") {
                    // Given
                    val to = Email("test@example.com")
                    val subject = "HTML Test"
                    val htmlBody = "<html><body><h1>Test</h1></body></html>"

                    // When
                    javaMailSender.send(to, subject, htmlBody)

                    // Then
                    fakeSpringMailSender.sentMimeMessages.size shouldBe 1
                    fakeSpringMailSender.sentSimpleMessages.size shouldBe 0
                }
            }

            context("메일 전송 중 오류가 발생했을 때") {
                it("MailSendingException을 던져야 한다") {
                    // Given
                    val to = Email("test@example.com")
                    val subject = "Test Subject"
                    val body = "Test Body"
                    val mailException = object : MailException("Mail server error") {}
                    fakeSpringMailSender.shouldThrowException = mailException

                    // When & Then
                    val exception =
                        shouldThrow<MailSendingException> {
                            javaMailSender.send(to, subject, body)
                        }

                    exception.message shouldContain "Failed to send email"
                    exception.cause shouldBe mailException
                }
            }

            context("제목이 비어있을 때") {
                it("IllegalArgumentException을 던져야 한다") {
                    // Given
                    val to = Email("test@example.com")
                    val subject = ""
                    val body = "Test Body"

                    // When & Then
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            javaMailSender.send(to, subject, body)
                        }

                    exception.message shouldContain "Subject cannot be empty"
                }
            }

            context("본문이 비어있을 때") {
                it("IllegalArgumentException을 던져야 한다") {
                    // Given
                    val to = Email("test@example.com")
                    val subject = "Test Subject"
                    val body = ""

                    // When & Then
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            javaMailSender.send(to, subject, body)
                        }

                    exception.message shouldContain "Body cannot be empty"
                }
            }
        }
    }
})
