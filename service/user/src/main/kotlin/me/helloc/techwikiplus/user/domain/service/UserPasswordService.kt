package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidPassword

interface UserPasswordService {

    /**
     * 비밀번호 유효성을 검사하는 메서드
     * @param password 검사할 비밀번호
     * @throws InvalidPassword 비밀번호가 유효하지 않은 경우 예외를 던짐
     * 비밀번호는 8-30자 사이여야 하며, 대문자, 소문자, 숫자, 특수문자를 포함해야 함
     */
    fun validate(password: String) {
        // Password must be 8-30 characters long and include uppercase, lowercase, numbers, and special characters
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,30}$")
        if (!passwordPattern.matches(password)) throw InvalidPassword(password)
    }
    /**
     * 비밀번호를 검증하고 암호화하는 메서드
     * @param password 평문 비밀번호
     * @return 암호화된 비밀번호
     * @throws InvalidPassword 비밀번호가 유효하지 않은 경우 예외를 던짐
     */
    fun validateAndEncode(password: String): String {
        validate(password)
        return encode(password)
    }

    /**
     * 비밀번호를 암호화하는 메서드
     * @param password 평문 비밀번호
     * @return 암호화된 비밀번호
     */
    fun encode(password: String): String

    /**
     * 암호화된 비밀번호와 평문 비밀번호를 비교하는 메서드
     * @param rawPassword 평문 비밀번호
     * @param encodedPassword 암호화된 비밀번호
     * @return 비밀번호가 일치하면 true, 그렇지 않으면 false
     */
    fun matches(rawPassword: String, encodedPassword: String): Boolean
}
