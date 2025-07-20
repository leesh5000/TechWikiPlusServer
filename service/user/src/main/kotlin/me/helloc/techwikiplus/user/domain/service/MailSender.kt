package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.VerificationCode

interface MailSender {

    /**
     * 이메일 인증을 위한 메일을 전송하는 메서드
     * @param email 인증할 이메일 주소
     * @return 생성된 인증 코드
     */
    fun sendVerificationEmail(email: String): VerificationCode

    /**
     * 비밀번호 재설정을 위한 메일을 전송하는 메서드
     * @param email 비밀번호를 재설정할 이메일 주소
     * @param code 재설정 코드
     */
    fun sendPasswordResetEmail(email: String, code: String)
}
