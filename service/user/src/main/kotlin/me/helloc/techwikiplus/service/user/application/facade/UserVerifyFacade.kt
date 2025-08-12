package me.helloc.techwikiplus.service.user.application.facade

import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.RegistrationCode
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.service.user.domain.service.UserModifier
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserVerifyUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class UserVerifyFacade(
    private val userReader: UserReader,
    private val emailVerifyService: EmailVerifyService,
    private val userModifier: UserModifier,
) : UserVerifyUseCase {
    override fun execute(
        email: Email,
        code: RegistrationCode,
    ) {
        // 1. 회원 가입 대기중인 사용자 조회
        val pendingUser: User = userReader.getPendingUser(email)

        // 2. 인증 코드가 유효한지 확인
        emailVerifyService.verify(pendingUser.email, code)

        // 3. 사용자 상태를 ACTIVE로 변경
        userModifier.activate(pendingUser)
    }
}
