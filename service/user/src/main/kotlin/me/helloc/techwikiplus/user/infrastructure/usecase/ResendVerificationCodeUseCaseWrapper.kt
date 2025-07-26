package me.helloc.techwikiplus.user.infrastructure.usecase

import me.helloc.techwikiplus.user.application.ResendVerificationCodeUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ResendVerificationCodeUseCaseWrapper(
    private val resendVerificationCodeUseCase: ResendVerificationCodeUseCase,
) {
    fun resendVerificationCode(email: String) {
        resendVerificationCodeUseCase.resendVerificationCode(email)
    }
}
