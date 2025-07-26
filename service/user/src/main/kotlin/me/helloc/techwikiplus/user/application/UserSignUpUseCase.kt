package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.UserRegister
import me.helloc.techwikiplus.user.domain.service.VerificationCodeSender
import org.slf4j.LoggerFactory

class UserSignUpUseCase(
    private val userRegister: UserRegister,
    private val verificationCodeSender: VerificationCodeSender,
    private val exceptionHandler: ApplicationExceptionHandler =
        ApplicationExceptionHandler(
            LoggerFactory.getLogger(UserSignUpUseCase::class.java),
        ),
) {
    /**
     * 사용자 회원가입을 처리한다
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
}
