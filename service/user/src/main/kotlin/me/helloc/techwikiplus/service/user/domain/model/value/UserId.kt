package me.helloc.techwikiplus.service.user.domain.model.value

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode

class UserId(val value: Long) {
    init {
        if (value <= 0) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_USER_ID_FORMAT,
                params = arrayOf("userId"),
            )
        }
        // Snowflake ID는 64비트 정수이므로 Long 타입의 최대값을 넘지 않음
        // 추가 검증이 필요하면 여기에 추가
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserId) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    companion object {
        fun from(value: String): UserId {
            try {
                return UserId(value.toLong())
            } catch (e: NumberFormatException) {
                throw DomainException(
                    errorCode = ErrorCode.INVALID_USER_ID_FORMAT,
                    params = arrayOf(value),
                    cause = e,
                )
            }
        }
    }
}
