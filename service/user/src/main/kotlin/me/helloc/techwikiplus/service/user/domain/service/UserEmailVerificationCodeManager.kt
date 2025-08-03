package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.port.EmailTemplateService
import me.helloc.techwikiplus.service.user.domain.service.port.MailSender
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore

class UserEmailVerificationCodeManager(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
    private val emailTemplateService: EmailTemplateService,
) {
    fun sendVerifyMailTo(user: User) {
        val verificationCode = VerificationCode.generate()
        val emailContent = emailTemplateService.createVerificationEmailContent(verificationCode)
        mailSender.send(user.email, emailContent.subject, emailContent.body)
        verificationCodeStore.store(user.email, verificationCode)
    }

    fun hasMailBeenSentTo(to: User): Boolean {
        return verificationCodeStore.exists(to.email)
    }
}
