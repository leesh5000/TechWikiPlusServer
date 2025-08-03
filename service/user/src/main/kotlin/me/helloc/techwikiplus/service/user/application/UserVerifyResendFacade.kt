package me.helloc.techwikiplus.service.user.application

import me.helloc.techwikiplus.service.user.domain.exception.UserNotPendingException
import me.helloc.techwikiplus.service.user.domain.service.UserEmailVerificationCodeManager
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserVerifyResendUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class UserVerifyResendFacade(
    private val userReader: UserReader,
    private val userEmailVerificationCodeManager: UserEmailVerificationCodeManager,
) : UserVerifyResendUseCase {
    override fun execute(command: UserVerifyResendUseCase.Command) {
        // 1. 사용자 조회
        val user = userReader.getBy(command.email)

        // 2. PENDING 상태 확인
        if (!user.isPending()) {
            throw UserNotPendingException(command.email.value)
        }

        // 3. 인증 메일 재전송
        userEmailVerificationCodeManager.sendVerifyMailTo(user)
    }
}
