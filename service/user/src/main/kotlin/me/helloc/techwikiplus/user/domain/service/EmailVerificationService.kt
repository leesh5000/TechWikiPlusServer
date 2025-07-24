package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.DomainConstants
import me.helloc.techwikiplus.user.domain.VerificationCode
import org.springframework.stereotype.Service

@Service
class EmailVerificationService(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
) {
    fun sendVerificationEmail(email: String): VerificationCode {
        val verificationCode = mailSender.sendVerificationEmail(email)
        verificationCodeStore.storeWithExpiry(email, verificationCode, DomainConstants.EmailVerification.CODE_TTL)
        return verificationCode
    }
}
