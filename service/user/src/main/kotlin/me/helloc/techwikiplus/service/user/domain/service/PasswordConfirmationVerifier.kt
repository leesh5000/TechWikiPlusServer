package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.PasswordMismatchException

class PasswordConfirmationVerifier {
    fun verify(
        rawPassword: String,
        rawConfirmPassword: String,
    ) {
        if (rawPassword != rawConfirmPassword) {
            throw PasswordMismatchException("Password and confirmation do not match.")
        }
    }
}
