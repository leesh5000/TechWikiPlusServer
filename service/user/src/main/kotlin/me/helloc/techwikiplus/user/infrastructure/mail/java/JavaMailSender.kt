package me.helloc.techwikiplus.user.infrastructure.mail.java

import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.domain.service.VerificationCode
import org.springframework.stereotype.Component

@Component
class JavaMailSender: MailSender {

    override fun sendVerificationEmail(email: String): VerificationCode {
        TODO("Not yet implemented")
    }

    override fun sendPasswordResetEmail(email: String, code: String) {
        TODO("Not yet implemented")
    }
}
