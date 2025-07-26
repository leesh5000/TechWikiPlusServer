package me.helloc.techwikiplus.user.infrastructure.usecase

import me.helloc.techwikiplus.user.application.VerifyEmailUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VerifyEmailUseCaseWrapper(
    private val verifyEmailUseCase: VerifyEmailUseCase,
) {
    fun verify(
        email: String,
        code: String,
    ) {
        verifyEmailUseCase.verify(email, code)
    }
}
