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
    /**
     * 커스텀 MailSender 빈을 생성합니다.
     * Spring Boot의 기본 mailSender 빈과 이름 충돌을 피하기 위해 customMailSender로 명명합니다.
     * spring.mail.type 프로퍼티에 따라 SmtpMailSender 또는 ConsoleMailSender를 반환합니다.
     */
    @Bean(name = ["customMailSender"])
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
