package me.helloc.techwikiplus.service.user.domain.model.value

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode

class UserId(value: String) {
    val value: String = value.trim()

    init {
        if (this.value.isBlank()) {
            throw DomainException(
                errorCode = ErrorCode.BLANK_USER_ID,
                params = arrayOf("userId")
            )
        }
        if (this.value.length > 64) {
            throw DomainException(
                errorCode = ErrorCode.USER_ID_TOO_LONG,
                params = arrayOf<Any>("userId", 64)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserId) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        fun from(value: String): UserId = UserId(value)
    }

    fun isNotBlank(): Boolean {
        return value.isNotBlank()
    }
}
