package me.helloc.techwikiplus.service.user.domain.model

import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode

class RawPassword(val value: String) {
    init {
        if (value.isBlank()) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.BLANK_PASSWORD,
                params = arrayOf("password"),
            )
        }
        if (value.length < MIN_LENGTH) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.PASSWORD_TOO_SHORT,
                params = arrayOf<Any>("password", MIN_LENGTH),
            )
        }
        if (value.length > MAX_LENGTH) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.PASSWORD_TOO_LONG,
                params = arrayOf<Any>("password", MAX_LENGTH),
            )
        }
        if (!value.any { it.isUpperCase() }) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.PASSWORD_NO_UPPERCASE,
                params = arrayOf("password"),
            )
        }
        if (!value.any { it.isLowerCase() }) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.PASSWORD_NO_LOWERCASE,
                params = arrayOf("password"),
            )
        }
        if (!SPECIAL_CHAR_REGEX.containsMatchIn(value)) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.PASSWORD_NO_SPECIAL_CHAR,
                params = arrayOf("password"),
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
