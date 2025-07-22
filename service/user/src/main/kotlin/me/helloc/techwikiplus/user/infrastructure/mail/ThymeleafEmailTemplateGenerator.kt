package me.helloc.techwikiplus.user.infrastructure.mail

import me.helloc.techwikiplus.user.domain.service.EmailTemplate
import me.helloc.techwikiplus.user.domain.service.EmailTemplateGenerator
import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Component
class ThymeleafEmailTemplateGenerator(
    private val templateEngine: TemplateEngine,
) : EmailTemplateGenerator {
    override fun generateVerificationEmail(code: String): EmailTemplate {
        val context =
            Context().apply {
                setVariable("code", code)
            }

        val body = templateEngine.process("email/verification-email", context)

        return EmailTemplate(
            subject = "TechWiki+ | 이메일 인증",
            body = body,
        )
    }

    override fun generatePasswordResetEmail(code: String): EmailTemplate {
        val context =
            Context().apply {
                setVariable("code", code)
            }

        val body = templateEngine.process("email/password-reset-email", context)

        return EmailTemplate(
            subject = "TechWiki+ 비밀번호 재설정",
            body = body,
        )
    }
}
