package me.helloc.techwikiplus.user.domain

data class VerificationCode(val code: String) {
    init {
        require(code.isNotBlank()) { "Verification code must not be blank." }
        require(code.length == 6) { "Verification code must be exactly 6 characters long." }
    }
}
