package me.helloc.techwikiplus.service.user.interfaces.usecase

import me.helloc.techwikiplus.service.user.domain.model.value.Email

interface UserVerifyResendUseCase {
    /**
     * 사용자 이메일로 인증 코드 재전송
     *
     * @param email 사용자 이메일
     */
    fun resend(email: Email)
}
