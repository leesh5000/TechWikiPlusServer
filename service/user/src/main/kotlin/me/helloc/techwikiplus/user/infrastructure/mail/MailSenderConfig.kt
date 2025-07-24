package me.helloc.techwikiplus.user.infrastructure.mail

import me.helloc.techwikiplus.user.domain.service.EmailTemplateGenerator
import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.infrastructure.mail.java.SmtpMailSender
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender

@Configuration
open class MailSenderConfig {
    @Bean
    open fun customMailSender(
        javaMailSender: JavaMailSender,
        emailTemplateGenerator: EmailTemplateGenerator,
    ): MailSender {
        return SmtpMailSender(javaMailSender, emailTemplateGenerator)
    }
}
