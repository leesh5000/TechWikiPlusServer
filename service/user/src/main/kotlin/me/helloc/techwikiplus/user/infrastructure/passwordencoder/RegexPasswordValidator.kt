package me.helloc.techwikiplus.user.infrastructure.passwordencoder

import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidPassword
import me.helloc.techwikiplus.user.domain.service.PasswordValidator
import org.springframework.stereotype.Component

@Component
class RegexPasswordValidator : PasswordValidator {
    companion object {
        /**
         * 비밀번호 검증 정규표현식
         * - 최소 8자, 최대 30자
         * - 대문자 최소 1개 포함 (?=.*[A-Z])
         * - 소문자 최소 1개 포함 (?=.*[a-z])
         * - 숫자 최소 1개 포함 (?=.*\d)
         * - 특수문자 최소 1개 포함 (?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?])
         */
        private val PASSWORD_PATTERN =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,30}\$")

        /**
         * 비밀번호 최소 길이
         */
        private const val MIN_LENGTH = 8

        /**
         * 비밀번호 최대 길이
         */
        private const val MAX_LENGTH = 30
    }

    override fun validate(password: String) {
        // ReDoS 방어를 위한 사전 길이 검증
        if (password.length < MIN_LENGTH || password.length > MAX_LENGTH) {
            throw InvalidPassword(password)
        }

        // 정규표현식을 통한 상세 검증
        if (!PASSWORD_PATTERN.matches(password)) {
            throw InvalidPassword(password)
        }
    }
}
