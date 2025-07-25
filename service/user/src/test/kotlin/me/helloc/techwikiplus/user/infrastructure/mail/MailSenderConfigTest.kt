package me.helloc.techwikiplus.user.infrastructure.mail

import me.helloc.techwikiplus.user.domain.service.EmailTemplateGenerator
import me.helloc.techwikiplus.user.infrastructure.mail.java.SmtpMailSender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.mail.javamail.JavaMailSender

class MailSenderConfigTest {
    private val javaMailSender: JavaMailSender = mock(JavaMailSender::class.java)
    private val emailTemplateGenerator: EmailTemplateGenerator = mock(EmailTemplateGenerator::class.java)

    @Test
    @DisplayName("항상 SmtpMailSender가 생성된다")
    fun shouldAlwaysCreateSmtpMailSender() {
        // Given
        val config = MailSenderConfig()

        // When
        val mailSender = config.mailSender(javaMailSender, emailTemplateGenerator)

        // Then
        assertThat(mailSender).isInstanceOf(SmtpMailSender::class.java)
    }
}
