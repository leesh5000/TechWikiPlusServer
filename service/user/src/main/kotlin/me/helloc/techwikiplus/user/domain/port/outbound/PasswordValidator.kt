package me.helloc.techwikiplus.user.domain.port.outbound

import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidPassword

interface PasswordValidator {
    /**
     * 비밀번호 유효성을 검사하는 메서드
     * @param password 검사할 비밀번호
     * @throws InvalidPassword 비밀번호가 유효하지 않은 경우 예외를 던짐
     */
    fun validate(password: String)
}
