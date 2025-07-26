package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.domain.service.UserWriter
import org.slf4j.LoggerFactory

class VerifyEmailUseCase(
    private val verificationCodeStore: VerificationCodeStore,
    private val userReader: UserReader,
    private val userWriter: UserWriter,
    private val exceptionHandler: ApplicationExceptionHandler =
        ApplicationExceptionHandler(
            LoggerFactory.getLogger(VerifyEmailUseCase::class.java),
        ),
) {
    /**
     * 이메일 인증을 처리한다
     * 인증 성공 시 사용자 상태를 ACTIVE로 변경한다
     *
     * @throws IllegalArgumentException 인증 코드가 존재하지 않거나 사용자를 찾을 수 없는 경우
     * @throws IllegalStateException 인증 코드가 일치하지 않는 경우
     * @throws InfrastructureException 데이터 저장 실패 시
     */
    fun verify(
        email: String,
        code: String,
    ) = exceptionHandler.execute("VerifyEmail") {
        val verificationCode: VerificationCode = verificationCodeStore.retrieveOrThrows(email)
        verificationCode.equalsOrThrows(code)
        val user: User = userReader.readByEmailOrThrows(email)
        val verifiedUser = user.completeSignUp()
        userWriter.insertOrUpdate(verifiedUser)
    }
}
