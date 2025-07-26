package me.helloc.techwikiplus.user.domain.exception.validation

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class AlreadyVerifiedEmailException(
    val email: String,
) : DomainException(
        errorCode = ErrorCode.ALREADY_VERIFIED_EMAIL,
        details = "Your input: $email",
    )
