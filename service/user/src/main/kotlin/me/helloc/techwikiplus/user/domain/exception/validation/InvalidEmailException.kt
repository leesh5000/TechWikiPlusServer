package me.helloc.techwikiplus.user.domain.exception.validation

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class InvalidEmailException(
    val email: String,
) : DomainException(
        errorCode = ErrorCode.INVALID_EMAIL,
        details = "Your input: $email",
    )
