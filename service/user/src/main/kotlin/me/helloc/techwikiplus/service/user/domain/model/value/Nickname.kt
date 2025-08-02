package me.helloc.techwikiplus.service.user.domain.model.value

import me.helloc.techwikiplus.service.user.domain.exception.NicknameValidationException

class Nickname(val value: String) {
    init {
        if (value.isBlank()) {
            throw NicknameValidationException(
                errorCode = NicknameValidationException.BLANK_NICKNAME,
                message = "닉네임은 필수 입력 항목입니다",
            )
        }
        if (value.length < MIN_LENGTH) {
            throw NicknameValidationException(
                errorCode = NicknameValidationException.TOO_SHORT,
                message = "닉네임은 최소 ${MIN_LENGTH}자 이상이어야 합니다",
            )
        }
        if (value.length > MAX_LENGTH) {
            throw NicknameValidationException(
                errorCode = NicknameValidationException.TOO_LONG,
                message = "닉네임은 최대 ${MAX_LENGTH}자 이하여야 합니다",
            )
        }
        if (value.contains(' ')) {
            throw NicknameValidationException(
                errorCode = NicknameValidationException.CONTAINS_SPACE,
                message = "닉네임에는 공백을 포함할 수 없습니다",
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
    }
}
