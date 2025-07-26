package me.helloc.techwikiplus.user.infrastructure.mail

import jakarta.mail.internet.MimeMessage
import me.helloc.techwikiplus.user.domain.port.outbound.EmailTemplateGenerator
import me.helloc.techwikiplus.user.domain.port.outbound.EmailTemplateGenerator.EmailTemplateDetails
import me.helloc.techwikiplus.user.infrastructure.exception.MailDeliveryException
import me.helloc.techwikiplus.user.infrastructure.mail.java.SmtpMailSender
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender

@DisplayName("SmtpMailSender 예외 변환 테스트")
class SmtpMailSenderExceptionTest {
    private lateinit var javaMailSender: JavaMailSender
    private lateinit var emailTemplateGenerator: EmailTemplateGenerator
    private lateinit var mailSender: SmtpMailSender
    private lateinit var mimeMessage: MimeMessage

    @BeforeEach
    fun setUp() {
        javaMailSender = mock()
        emailTemplateGenerator = mock()
        mailSender = SmtpMailSender(javaMailSender, emailTemplateGenerator)
        mimeMessage = mock()

        // Set up default behavior
        `when`(emailTemplateGenerator.generateVerificationEmail(any(String::class.java))).thenReturn(
            EmailTemplateDetails("Test Subject", "<html>Test Body</html>"),
        )
        `when`(emailTemplateGenerator.generatePasswordResetEmail(any(String::class.java))).thenReturn(
            EmailTemplateDetails("Reset Subject", "<html>Reset Body</html>"),
        )
    }

    @Test
    @DisplayName("sendVerificationEmail에서 메일 발송 실패 시 MailDeliveryException으로 변환")
    fun `converts mail send exception to MailDeliveryException on sendVerificationEmail`() {
        // given
        val email = "user@example.com"
        val mailException = MailSendException("SMTP server not available")

        `when`(javaMailSender.createMimeMessage()).thenReturn(mimeMessage)
        `when`(javaMailSender.send(any(MimeMessage::class.java))).thenThrow(mailException)

        // when & then
        assertThatThrownBy { mailSender.sendVerificationEmail(email) }
            .isInstanceOf(MailDeliveryException::class.java)
            .hasMessageContaining(email)
            .hasCause(mailException)
            .extracting("retryable")
            .isEqualTo(true)
    }

    @Test
    @DisplayName("sendPasswordResetEmail에서 메일 발송 실패 시 MailDeliveryException으로 변환")
    fun `converts mail send exception to MailDeliveryException on sendPasswordResetEmail`() {
        // given
        val email = "user@example.com"
        val code = "123456"
        val mailException = RuntimeException("Authentication failed")

        `when`(javaMailSender.createMimeMessage()).thenReturn(mimeMessage)
        `when`(javaMailSender.send(any(MimeMessage::class.java))).thenThrow(mailException)

        // when & then
        assertThatThrownBy { mailSender.sendPasswordResetEmail(email, code) }
            .isInstanceOf(MailDeliveryException::class.java)
            .hasMessageContaining(email)
            .hasCause(mailException)
    }

    @Test
    @DisplayName("MimeMessage 생성 실패 시 MailDeliveryException으로 변환")
    fun `converts mime message creation exception to MailDeliveryException`() {
        // given
        val email = "user@example.com"
        val messageException = RuntimeException("Failed to create message")

        `when`(javaMailSender.createMimeMessage()).thenThrow(messageException)

        // when & then
        assertThatThrownBy { mailSender.sendVerificationEmail(email) }
            .isInstanceOf(MailDeliveryException::class.java)
            .hasMessageContaining(email)
            .hasCause(messageException)
    }
}
