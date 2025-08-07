package me.helloc.techwikiplus.service.user.application.service

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.service.user.domain.service.UserModifier
import me.helloc.techwikiplus.service.user.domain.service.UserRegister
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserSignUpUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class UserSignUpFacade(
    private val userRegister: UserRegister,
    private val emailVerifyService: EmailVerifyService,
    private val userModifier: UserModifier,
) : UserSignUpUseCase {
    override fun execute(
        email: Email,
        nickname: Nickname,
        password: RawPassword,
        confirmPassword: RawPassword,
    ) {
        val user =
            userRegister.insert(
                email = email,
                nickname = nickname,
                password = password,
                passwordConfirm = confirmPassword,
            )
        emailVerifyService.startVerification(user)
        userModifier.setPending(user)
    }
}
