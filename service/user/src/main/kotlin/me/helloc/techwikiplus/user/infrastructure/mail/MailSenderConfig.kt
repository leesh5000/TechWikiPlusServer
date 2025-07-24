package me.helloc.techwikiplus.user.infrastructure.mail

import me.helloc.techwikiplus.user.domain.service.EmailTemplateGenerator
import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.infrastructure.mail.console.ConsoleMailSender
import me.helloc.techwikiplus.user.infrastructure.mail.java.SmtpMailSender
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender

@Configuration
@EnableConfigurationProperties(MailProperties::class)
open class MailSenderConfig(
    private val mailProperties: MailProperties,
) {
    @Bean
    open fun mailSender(
        javaMailSender: JavaMailSender,
        emailTemplateGenerator: EmailTemplateGenerator,
    ): MailSender {
        return when (mailProperties.type) {
            "smtp" -> SmtpMailSender(javaMailSender, emailTemplateGenerator)
            else -> ConsoleMailSender()
        }
    }
}
