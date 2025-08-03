package me.helloc.techwikiplus.service.user.domain.model.value

import me.helloc.techwikiplus.service.user.domain.exception.PasswordValidationException

class RawPassword(val value: String) {
    init {
        if (value.isBlank()) {
            throw PasswordValidationException(
                errorCode = PasswordValidationException.BLANK_PASSWORD,
                message = "비밀번호는 필수 입력 항목입니다",
            )
        }
        if (value.length < MIN_LENGTH) {
            throw PasswordValidationException(
                errorCode = PasswordValidationException.TOO_SHORT,
                message = "비밀번호는 최소 ${MIN_LENGTH}자 이상이어야 합니다",
            )
        }
        if (value.length > MAX_LENGTH) {
            throw PasswordValidationException(
                errorCode = PasswordValidationException.TOO_LONG,
                message = "비밀번호는 최대 ${MAX_LENGTH}자 이하여야 합니다",
            )
        }
        if (!value.any { it.isUpperCase() }) {
            throw PasswordValidationException(
                errorCode = PasswordValidationException.NO_UPPERCASE,
                message = "비밀번호는 대문자를 포함해야 합니다",
            )
        }
        if (!value.any { it.isLowerCase() }) {
            throw PasswordValidationException(
                errorCode = PasswordValidationException.NO_LOWERCASE,
                message = "비밀번호는 소문자를 포함해야 합니다",
            )
        }
        if (!SPECIAL_CHAR_REGEX.containsMatchIn(value)) {
            throw PasswordValidationException(
                errorCode = PasswordValidationException.NO_SPECIAL_CHAR,
                message = "비밀번호는 특수문자를 포함해야 합니다",
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawPassword) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "RawPassword(****)"
    }

    companion object {
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 30
        private val SPECIAL_CHAR_REGEX = Regex("[!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>?/~`]")
    }
}
