package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.domain.service.PendingUserValidator
import me.helloc.techwikiplus.user.domain.service.VerificationCodeStore
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ResendVerificationCodeUseCase(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
    private val pendingUserValidator: PendingUserValidator,
) {
    fun resendVerificationCode(email: String) {
        // 현재 "PENDING" 상태의 사용자만 재전송 가능
        pendingUserValidator.existsOrThrows(email)
        val verificationCode: VerificationCode = mailSender.sendVerificationEmail(email)
        val ttl: Duration = Duration.ofMinutes(5)
        verificationCodeStore.storeWithExpiry(email, verificationCode, ttl)
    }
}
