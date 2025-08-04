package me.helloc.techwikiplus.service.user.domain.model.value

import me.helloc.techwikiplus.service.user.domain.exception.EmailValidationException

class Email(value: String) {
    val value: String = value.lowercase()

    init {
        if (this.value.isBlank()) {
            throw EmailValidationException(
                errorCode = EmailValidationException.BLANK_EMAIL,
                message = "이메일은 필수 입력 항목입니다",
            )
        }
        if (!EMAIL_REGEX.matches(this.value)) {
            throw EmailValidationException(
                errorCode = EmailValidationException.INVALID_FORMAT,
                message = "올바른 이메일 형식이 아닙니다",
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Email) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "Email(value=$value)"
    }

    companion object {
        private val EMAIL_REGEX = """^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\.[A-Za-z]{2,})$""".toRegex()
    }
}
