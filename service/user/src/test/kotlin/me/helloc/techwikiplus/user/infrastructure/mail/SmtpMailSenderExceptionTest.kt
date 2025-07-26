package me.helloc.techwikiplus.user.infrastructure.mail

import me.helloc.techwikiplus.user.infrastructure.exception.MailDeliveryException
import me.helloc.techwikiplus.user.infrastructure.mail.fake.FakeEmailTemplateGenerator
import me.helloc.techwikiplus.user.infrastructure.mail.fake.FakeJavaMailSender
import me.helloc.techwikiplus.user.infrastructure.mail.java.SmtpMailSender
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mail.MailSendException

@DisplayName("SmtpMailSender 예외 변환 테스트")
class SmtpMailSenderExceptionTest {
    private lateinit var fakeJavaMailSender: FakeJavaMailSender
    private lateinit var fakeEmailTemplateGenerator: FakeEmailTemplateGenerator
    private lateinit var mailSender: SmtpMailSender

    @BeforeEach
    fun setUp() {
        fakeJavaMailSender = FakeJavaMailSender()
        fakeEmailTemplateGenerator = FakeEmailTemplateGenerator()
        mailSender = SmtpMailSender(fakeJavaMailSender, fakeEmailTemplateGenerator, "test@example.com")
    }

    @Test
    @DisplayName("sendVerificationEmail에서 메일 발송 실패 시 MailDeliveryException으로 변환")
    fun `converts mail send exception to MailDeliveryException on sendVerificationEmail`() {
        // given
        val email = "user@example.com"
        val mailException = MailSendException("SMTP server not available")
        fakeJavaMailSender.setShouldThrowOnSend(mailException)

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
        fakeJavaMailSender.setShouldThrowOnSend(mailException)

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
        fakeJavaMailSender.setShouldThrowOnCreateMessage(messageException)

        // when & then
        assertThatThrownBy { mailSender.sendVerificationEmail(email) }
            .isInstanceOf(MailDeliveryException::class.java)
            .hasMessageContaining(email)
            .hasCause(messageException)
    }
}
