package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.EmailVerificationService
import me.helloc.techwikiplus.user.domain.service.UserRegistrationService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
open class UserSignUpUseCase(
    private val userRegistrationService: UserRegistrationService,
    private val emailVerificationService: EmailVerificationService,
) {
    fun signUp(
        email: String,
        nickname: String,
        password: String,
    ) {
        userRegistrationService.registerPendingUser(email, nickname, password)
        emailVerificationService.sendVerificationEmail(email)
    }
}
