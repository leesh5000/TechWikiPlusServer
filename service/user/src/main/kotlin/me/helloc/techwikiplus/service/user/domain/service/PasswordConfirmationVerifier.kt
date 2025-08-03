package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.PasswordMismatchException
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword

class PasswordConfirmationVerifier {
    fun verify(
        rawPassword: RawPassword,
        rawConfirmPassword: RawPassword,
    ) {
        if (rawPassword != rawConfirmPassword) {
            throw PasswordMismatchException("Password and confirmation do not match.")
        }
    }
}
