package me.helloc.techwikiplus.service.user.domain.model.value

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode

class Nickname(val value: String) {
    init {
        if (value.isBlank()) {
            throw DomainException(
                errorCode = ErrorCode.BLANK_NICKNAME,
                params = arrayOf("nickname")
            )
        }
        if (value.length < MIN_LENGTH) {
            throw DomainException(
                errorCode = ErrorCode.NICKNAME_TOO_SHORT,
                params = arrayOf<Any>("nickname", MIN_LENGTH)
            )
        }
        if (value.length > MAX_LENGTH) {
            throw DomainException(
                errorCode = ErrorCode.NICKNAME_TOO_LONG,
                params = arrayOf<Any>("nickname", MAX_LENGTH)
            )
        }
        if (value.contains(' ')) {
            throw DomainException(
                errorCode = ErrorCode.NICKNAME_CONTAINS_SPACE,
                params = arrayOf("nickname")
            )
        }
        if (!value.matches(ALLOWED_PATTERN)) {
            throw DomainException(
                errorCode = ErrorCode.NICKNAME_CONTAINS_SPECIAL_CHAR,
                params = arrayOf("nickname")
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Nickname) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "Nickname(value=$value)"
    }

    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 20
        private val ALLOWED_PATTERN = "^[가-힣a-zA-Z0-9_-]+$".toRegex()
    }
}
