package me.helloc.techwikiplus.user.domain

import me.helloc.techwikiplus.user.domain.exception.CustomException.AuthenticationException.InvalidVerificationCode

data class VerificationCode(val value: String) {

    companion object {
        const val LENGTH = 6
        /**
         * Generates a random verification code of the specified length.
         * The code consists of alphanumeric characters.
         */
        fun generate(): VerificationCode {
            return (1..LENGTH)
                .map { ('A'..'Z').random() }
                .joinToString("")
                .let { VerificationCode(it) }
        }
    }

    init {
        require(value.isNotBlank()) { "Verification code must not be blank." }
        require(value.length == 6) { "Verification code must be exactly 6 characters long." }
        require(value.all { it.isLetterOrDigit() }) { "Verification code must contain only letters and digits." }
    }

    fun equalsOrThrows(code: String) {
        if (this.value != code) {
            throw InvalidVerificationCode(code)
        }
    }
}
