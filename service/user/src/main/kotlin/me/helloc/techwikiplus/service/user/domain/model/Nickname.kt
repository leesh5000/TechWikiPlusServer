package me.helloc.techwikiplus.service.user.domain.model.value

import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode

/**
 * Nickname 정책
 * - 저장할 때는 대소문자를 구분하여 저장하고, 비교/조회할 때는 소문자로 변환하여 비교/조회한다.
 * - 사용자에게 보여줄 때는 그대로 보여준다.
 */
class Nickname(val value: String) {
    init {
        if (value.isBlank()) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.BLANK_NICKNAME,
                params = arrayOf("nickname"),
            )
        }
        if (value.length < MIN_LENGTH) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.NICKNAME_TOO_SHORT,
                params = arrayOf<Any>("nickname", MIN_LENGTH),
            )
        }
        if (value.length > MAX_LENGTH) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.NICKNAME_TOO_LONG,
                params = arrayOf<Any>("nickname", MAX_LENGTH),
            )
        }
        if (value.contains(' ')) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.NICKNAME_CONTAINS_SPACE,
                params = arrayOf("nickname"),
            )
        }
        if (!value.matches(ALLOWED_PATTERN)) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.NICKNAME_CONTAINS_SPECIAL_CHAR,
                params = arrayOf("nickname"),
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
