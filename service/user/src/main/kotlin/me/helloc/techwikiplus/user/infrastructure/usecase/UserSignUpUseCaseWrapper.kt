package me.helloc.techwikiplus.user.infrastructure.usecase

import me.helloc.techwikiplus.user.application.UserSignUpUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserSignUpUseCaseWrapper(
    private val userSignUpUseCase: UserSignUpUseCase,
) {
    fun signUp(
        email: String,
        nickname: String,
        password: String,
    ) {
        userSignUpUseCase.signUp(email, nickname, password)
    }
}
