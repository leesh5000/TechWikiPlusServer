package me.helloc.techwikiplus.service.user.domain.model

import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode

class Email(value: String) {
    val value: String = value.lowercase()

    init {
        if (this.value.isBlank()) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.BLANK_EMAIL,
                params = arrayOf("email"),
            )
        }
        if (!EMAIL_REGEX.matches(this.value)) {
            throw UserDomainException(
                userErrorCode = UserErrorCode.INVALID_EMAIL_FORMAT,
                params = arrayOf("email"),
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
