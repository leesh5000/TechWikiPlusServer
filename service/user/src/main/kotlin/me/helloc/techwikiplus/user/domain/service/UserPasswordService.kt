package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidPassword

interface UserPasswordService {

    fun validate(password: String) {
        // Password must be 8-30 characters long and include uppercase, lowercase, numbers, and special characters
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,30}$")
        if (!passwordPattern.matches(password)) throw InvalidPassword(password)
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
