package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.domain.service.PendingUserValidator
import org.slf4j.LoggerFactory
import java.time.Duration

class ResendVerificationCodeUseCase(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
    private val pendingUserValidator: PendingUserValidator,
    private val exceptionHandler: ApplicationExceptionHandler =
        ApplicationExceptionHandler(
            LoggerFactory.getLogger(ResendVerificationCodeUseCase::class.java),
        ),
) {
    companion object {
        private const val VERIFICATION_CODE_TTL_MINUTES = 5L
    }

    /**
     * 인증 코드를 재발송한다
     * PENDING 상태의 사용자만 재발송 가능하다
     *
     * @throws IllegalStateException 사용자가 PENDING 상태가 아닌 경우
     * @throws InfrastructureException 이메일 발송 또는 저장 실패 시
     */
    fun resendVerificationCode(email: String) {
        exceptionHandler.execute("ResendVerificationCode") {
            // 현재 "PENDING" 상태의 사용자만 재전송 가능
            pendingUserValidator.existsOrThrows(email)
            val verificationCode: VerificationCode = mailSender.sendVerificationEmail(email)
            val ttl: Duration = Duration.ofMinutes(VERIFICATION_CODE_TTL_MINUTES)
            verificationCodeStore.storeWithExpiry(email, verificationCode, ttl)
        }
    }
}
