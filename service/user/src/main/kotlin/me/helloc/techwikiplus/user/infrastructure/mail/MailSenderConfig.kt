package me.helloc.techwikiplus.user.infrastructure.mail

import me.helloc.techwikiplus.user.domain.port.outbound.EmailTemplateGenerator
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.infrastructure.mail.java.SmtpMailSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender

@Configuration
@EnableConfigurationProperties(MailProperties::class)
open class MailSenderConfig {
    /**
     * 커스텀 MailSender 빈을 생성합니다.
     * Spring Boot의 기본 mailSender 빈과 이름 충돌을 피하기 위해 customMailSender로 명명합니다.
     * SmtpMailSender를 반환합니다.
     */
    @Bean(name = ["customMailSender"])
    open fun mailSender(
        javaMailSender: JavaMailSender,
        emailTemplateGenerator: EmailTemplateGenerator,
        @Value("\${spring.mail.username}") from: String,
    ): MailSender {
        return SmtpMailSender(javaMailSender, emailTemplateGenerator, from)
    }
}
