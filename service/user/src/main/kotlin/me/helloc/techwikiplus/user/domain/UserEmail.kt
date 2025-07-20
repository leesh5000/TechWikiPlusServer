package me.helloc.techwikiplus.user.domain

import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidEmail

data class UserEmail(val value: String, val verified: Boolean = false) {
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

        fun isValid(email: String): Boolean = EMAIL_REGEX.matches(email)
    }

    init {
        if (!isValid(value)) {
            throw InvalidEmail(value)
        }
    }

    fun verify(): UserEmail {
        return UserEmail(
            value,
            true,
        )
    }

    override fun toString(): String = value
}
