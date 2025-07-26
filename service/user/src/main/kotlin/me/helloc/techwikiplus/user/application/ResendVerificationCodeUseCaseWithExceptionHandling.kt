package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.domain.service.PendingUserValidator
import java.time.Duration

/**
 * 예외 처리가 강화된 인증 코드 재발송 UseCase
 * 이메일 발송과 코드 저장을 트랜잭션 단위로 처리한다
 */
class ResendVerificationCodeUseCaseWithExceptionHandling(
    private val mailSender: MailSender,
    private val verificationCodeStore: VerificationCodeStore,
    private val pendingUserValidator: PendingUserValidator,
    private val exceptionHandler: ApplicationExceptionHandler,
) {
    companion object {
        private const val VERIFICATION_CODE_TTL_MINUTES = 5L
    }

    /**
     * 인증 코드를 재발송한다
     * PENDING 상태의 사용자만 재발송 가능하다
     *
     * @throws IllegalStateException 사용자가 PENDING 상태가 아닌 경우
     * @throws MailDeliveryException 이메일 발송 실패 시
     * @throws ExternalServiceException 인증 코드 저장 실패 시
     */
    fun resendVerificationCode(email: String) {
        exceptionHandler.execute("ResendVerificationCode") {
            // 1. 사용자 상태 검증
            pendingUserValidator.existsOrThrows(email)

            // 2. 이메일 발송
            val verificationCode = mailSender.sendVerificationEmail(email)

            // 3. 인증 코드 저장
            val ttl = Duration.ofMinutes(VERIFICATION_CODE_TTL_MINUTES)
            verificationCodeStore.storeWithExpiry(email, verificationCode, ttl)
        }
    }

    /**
     * 재시도 로직이 포함된 인증 코드 재발송
     * 이메일 발송 실패 시 지정된 횟수만큼 재시도한다
     *
     * @param maxRetries 최대 재시도 횟수 (기본값: 3)
     * @throws IllegalStateException 사용자가 PENDING 상태가 아닌 경우
     * @throws RetryExhaustedException 최대 재시도 횟수 초과 시
     * @throws ExternalServiceException 인증 코드 저장 실패 시
     */
    fun resendVerificationCodeWithRetry(
        email: String,
        maxRetries: Int = 3,
    ) {
        exceptionHandler.execute("ResendVerificationCode") {
            // 1. 사용자 상태 검증
            pendingUserValidator.existsOrThrows(email)

            // 2. 재시도 로직을 포함한 이메일 발송
            val verificationCode =
                exceptionHandler.executeWithRetry(
                    useCaseName = "ResendVerificationCode-EmailSend",
                    action = { mailSender.sendVerificationEmail(email) },
                    maxAttempts = maxRetries,
                    delayMillis = 500,
                )

            // 3. 인증 코드 저장
            val ttl = Duration.ofMinutes(VERIFICATION_CODE_TTL_MINUTES)
            verificationCodeStore.storeWithExpiry(email, verificationCode, ttl)
        }
    }
}
