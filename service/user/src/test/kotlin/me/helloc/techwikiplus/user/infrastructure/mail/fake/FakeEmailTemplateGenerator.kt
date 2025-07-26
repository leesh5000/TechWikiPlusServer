package me.helloc.techwikiplus.user.infrastructure.mail.fake

import me.helloc.techwikiplus.user.domain.port.outbound.EmailTemplateGenerator
import me.helloc.techwikiplus.user.domain.port.outbound.EmailTemplateGenerator.EmailTemplateDetails

class FakeEmailTemplateGenerator : EmailTemplateGenerator {
    override fun generateVerificationEmail(code: String): EmailTemplateDetails {
        return EmailTemplateDetails(
            subject = "Verify your email",
            body = "Your verification code is: $code",
        )
    }

    override fun generatePasswordResetEmail(code: String): EmailTemplateDetails {
        return EmailTemplateDetails(
            subject = "Reset your password",
            body = "Your password reset code is: $code",
        )
    }
}
