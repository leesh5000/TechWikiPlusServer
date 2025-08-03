package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.port.MailSender
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore

class UserEmailVerificationCodeManager(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
) {
    fun sendVerifyMailTo(user: User) {
        val verificationCode = VerificationCode.generate()
        val subject = "TechWiki+ 이메일 인증 코드"
        val body = "인증 코드는 $verificationCode 입니다."
        mailSender.send(user.email, subject, body)
        verificationCodeStore.store(user.email, verificationCode)
    }

    fun hasMailBeenSentTo(to: User): Boolean {
        return verificationCodeStore.exists(to.email)
    }
}
