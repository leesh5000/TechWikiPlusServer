package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.UserRegister
import me.helloc.techwikiplus.user.domain.service.VerificationCodeSender
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
open class UserSignUpUseCase(
    private val userRegister: UserRegister,
    private val verificationCodeSender: VerificationCodeSender,
) {
    fun signUp(
        email: String,
        nickname: String,
        password: String,
    ) {
        userRegister.registerPendingUser(email, nickname, password)
        verificationCodeSender.sendMail(email)
    }
}
