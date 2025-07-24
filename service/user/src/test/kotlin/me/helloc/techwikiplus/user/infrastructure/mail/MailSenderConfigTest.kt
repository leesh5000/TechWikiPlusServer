package me.helloc.techwikiplus.user.infrastructure.mail

import org.mockito.Mockito.mock
import me.helloc.techwikiplus.user.domain.service.EmailTemplateGenerator
import me.helloc.techwikiplus.user.infrastructure.mail.console.ConsoleMailSender
import me.helloc.techwikiplus.user.infrastructure.mail.java.SmtpMailSender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender

class MailSenderConfigTest {
    private val javaMailSender: JavaMailSender = mock(JavaMailSender::class.java)
    private val emailTemplateGenerator: EmailTemplateGenerator = mock(EmailTemplateGenerator::class.java)

    @Test
    @DisplayName("spring.mail.type이 smtp일 때 SmtpMailSender가 생성된다")
    fun shouldCreateSmtpMailSenderWhenTypeIsSmtp() {
        // Given
        val mailProperties = MailProperties(type = "smtp")
        val config = MailSenderConfig(mailProperties)

        // When
        val mailSender = config.mailSender(javaMailSender, emailTemplateGenerator)

        // Then
        assertThat(mailSender).isInstanceOf(SmtpMailSender::class.java)
    }

    @Test
    @DisplayName("spring.mail.type이 console일 때 ConsoleMailSender가 생성된다")
    fun shouldCreateConsoleMailSenderWhenTypeIsConsole() {
        // Given
        val mailProperties = MailProperties(type = "console")
        val config = MailSenderConfig(mailProperties)

        // When
        val mailSender = config.mailSender(javaMailSender, emailTemplateGenerator)

        // Then
        assertThat(mailSender).isInstanceOf(ConsoleMailSender::class.java)
    }

    @Test
    @DisplayName("spring.mail.type이 알 수 없는 값일 때 ConsoleMailSender가 생성된다")
    fun shouldCreateConsoleMailSenderWhenTypeIsUnknown() {
        // Given
        val mailProperties = MailProperties(type = "unknown")
        val config = MailSenderConfig(mailProperties)

        // When
        val mailSender = config.mailSender(javaMailSender, emailTemplateGenerator)

        // Then
        assertThat(mailSender).isInstanceOf(ConsoleMailSender::class.java)
    }

    @Test
    @DisplayName("기본값으로 ConsoleMailSender가 생성된다")
    fun shouldCreateConsoleMailSenderByDefault() {
        // Given
        val mailProperties = MailProperties() // 기본값은 console
        val config = MailSenderConfig(mailProperties)

        // When
        val mailSender = config.mailSender(javaMailSender, emailTemplateGenerator)

        // Then
        assertThat(mailSender).isInstanceOf(ConsoleMailSender::class.java)
    }
}

