package me.helloc.techwikiplus.user.domain.service.fake

import me.helloc.techwikiplus.user.domain.service.EmailTemplate
import me.helloc.techwikiplus.user.domain.service.EmailTemplateGenerator

class FakeEmailTemplateGenerator : EmailTemplateGenerator {
    override fun generateVerificationEmail(code: String): EmailTemplate {
        return EmailTemplate(
            subject = "Verify your email",
            body = "Your verification code is: $code"
        )
    }

    override fun generatePasswordResetEmail(code: String): EmailTemplate {
        return EmailTemplate(
            subject = "Reset your password",
            body = "Your password reset code is: $code"
        )
    }
}