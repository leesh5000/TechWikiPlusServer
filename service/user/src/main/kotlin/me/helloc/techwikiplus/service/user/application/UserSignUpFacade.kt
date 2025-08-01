package me.helloc.techwikiplus.service.user.application

import me.helloc.techwikiplus.application.usecase.UserSignUpUseCase
import me.helloc.techwikiplus.domain.service.UserReader
import org.springframework.stereotype.Component

@Component
class UserSignUpFacade(
    private val reader: UserReader
) : UserSignUpUseCase {
    override fun signup(
        email: String,
        password: String,
        confirmPassword: String,
        nickname: String,
    ): Boolean {
        return false
    }
}
