package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.DomainConstants
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore

class VerificationCodeSender(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
) {
    fun sendMail(email: String): VerificationCode {
        val verificationCode = mailSender.sendVerificationEmail(email)
        verificationCodeStore.storeWithExpiry(email, verificationCode, DomainConstants.EmailVerification.CODE_TTL)
        return verificationCode
    }
}
