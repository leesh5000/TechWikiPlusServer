package me.helloc.techwikiplus.user.infrastructure.mail

import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.infrastructure.mail.console.ConsoleMailSender
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class MailSenderConfig {
    @Bean
    open fun customMailSender(): MailSender {
        return ConsoleMailSender()
    }
}
