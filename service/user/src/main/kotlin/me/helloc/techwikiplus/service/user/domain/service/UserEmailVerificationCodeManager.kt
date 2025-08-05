package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.port.EmailTemplatePrinter
import me.helloc.techwikiplus.service.user.domain.service.port.MailSender
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore

class UserEmailVerificationCodeManager(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
    private val emailTemplatePrinter: EmailTemplatePrinter,
) {
    fun sendVerifyMailTo(user: User) {
        val verificationCode = VerificationCode.generate()
        val emailContent = emailTemplatePrinter.createVerificationEmailContent(verificationCode)
        mailSender.send(user.email, emailContent.subject, emailContent.body)
        verificationCodeStore.store(user.email, verificationCode)
    }

    fun hasMailBeenSentTo(to: User): Boolean {
        return verificationCodeStore.exists(to.email)
    }
}
