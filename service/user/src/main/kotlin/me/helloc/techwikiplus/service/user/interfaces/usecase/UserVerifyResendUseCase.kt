package me.helloc.techwikiplus.service.user.interfaces.usecase

import me.helloc.techwikiplus.service.user.domain.model.value.Email

interface UserVerifyResendUseCase {
    /**
     * 사용자 이메일로 인증 코드 재전송
     *
     * @param command 인증 코드 재전송에 필요한 정보를 담은 커맨드 객체
     */
    fun execute(command: Command)

    data class Command(
        val email: Email,
    )
}
