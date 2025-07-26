package me.helloc.techwikiplus.user.infrastructure.usecase

import me.helloc.techwikiplus.user.application.TokenResult
import me.helloc.techwikiplus.user.application.UserLoginUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserLoginUseCaseWrapper(
    private val userLoginUseCase: UserLoginUseCase,
) {
    fun login(
        email: String,
        password: String,
    ): TokenResult {
        return userLoginUseCase.login(email, password)
    }
}
