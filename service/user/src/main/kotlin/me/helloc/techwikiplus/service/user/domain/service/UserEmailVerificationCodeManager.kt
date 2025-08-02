package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.port.MailSender
import me.helloc.techwikiplus.service.user.domain.service.port.VerificationCodeStore
import java.time.Duration
import java.time.temporal.ChronoUnit.MINUTES

class UserEmailVerificationCodeManager(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
) {
    fun sendVerifyMailTo(user: User) {
        val verificationCode = VerificationCode.generate()
        val subject = "TechWiki+ 이메일 인증 코드"
        val body = "인증 코드는 $verificationCode 입니다."
        mailSender.send(user.email, subject, body)
        val key: String = EMAIL_VERIFICATION_CODE_KEY_FORMAT.format(user.email.value)
        verificationCodeStore.set(
            key,
            verificationCode.value,
            EMAIL_VERIFICATION_CODE_TTL,
        )
    }

    fun hasMailBeenSentTo(to: User): Boolean {
        val key: String = EMAIL_VERIFICATION_CODE_KEY_FORMAT.format(to.email.value)
        return verificationCodeStore.exists(key)
    }

    companion object {
        const val EMAIL_VERIFICATION_CODE_KEY_FORMAT = "user-service:user:email:%s"
        val EMAIL_VERIFICATION_CODE_TTL: Duration = Duration.of(5, MINUTES)
    }
}
