package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.UserRegister
import me.helloc.techwikiplus.user.domain.service.VerificationCodeSender

/**
 * 예외 처리가 강화된 회원가입 UseCase
 * 사용자 등록과 이메일 발송을 트랜잭션 단위로 처리하며,
 * 이메일 발송 실패 시에도 사용자 등록은 유지된다
 */
class UserSignUpUseCaseWithExceptionHandling(
    private val userRegister: UserRegister,
    private val verificationCodeSender: VerificationCodeSender,
    private val exceptionHandler: ApplicationExceptionHandler,
) {
    /**
     * 회원가입을 처리한다
     * 사용자 등록 후 인증 코드 이메일을 발송한다
     *
     * @throws DataAccessException 사용자 등록 실패 시
     * @throws CompensationFailedException 사용자는 등록되었으나 이메일 발송 실패 시
     */
    fun signUp(
        email: String,
        nickname: String,
        password: String,
    ) {
        exceptionHandler.executeWithCompensation(
            useCaseName = "UserSignUp",
            mainAction = {
                userRegister.registerPendingUser(email, nickname, password)
            },
            compensationAction = {
                verificationCodeSender.sendMail(email)
            },
        )
    }

    /**
     * 재시도 로직이 포함된 회원가입을 처리한다
     * 이메일 발송 실패 시 지정된 횟수만큼 재시도한다
     *
     * @param maxRetries 최대 재시도 횟수 (기본값: 3)
     * @throws DataAccessException 사용자 등록 실패 시
     * @throws CompensationFailedException 최종적으로 이메일 발송 실패 시
     */
    fun signUpWithRetry(
        email: String,
        nickname: String,
        password: String,
        maxRetries: Int = 3,
    ) {
        exceptionHandler.executeWithCompensation(
            useCaseName = "UserSignUp",
            mainAction = {
                userRegister.registerPendingUser(email, nickname, password)
            },
            compensationAction = {
                exceptionHandler.executeWithRetry(
                    useCaseName = "UserSignUp-EmailSend",
                    action = { verificationCodeSender.sendMail(email) },
                    maxAttempts = maxRetries,
                    delayMillis = 500,
                )
            },
        )
    }
}
