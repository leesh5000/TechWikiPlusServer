package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.application.port.outbound.EmailTemplatePrinter
import me.helloc.techwikiplus.service.user.application.port.outbound.MailSender
import me.helloc.techwikiplus.service.user.application.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode

class UserEmailVerificationCodeManager(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
    private val emailTemplatePrinter: EmailTemplatePrinter,
) {
    fun sendVerifyMailTo(user: User) {
        val registrationCode = RegistrationCode.generate()
        val emailContent = emailTemplatePrinter.createVerificationEmailContent(registrationCode)
        mailSender.send(user.email, emailContent.subject, emailContent.body)
        verificationCodeStore.store(user.email, registrationCode)
    }

    fun hasMailBeenSentTo(to: User): Boolean {
        return verificationCodeStore.exists(to.email)
    }
}
