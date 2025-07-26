package me.helloc.techwikiplus.user.domain.exception.conflict

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class DuplicateEmailException(
    val email: String,
) : DomainException(
        errorCode = ErrorCode.DUPLICATE_EMAIL,
        details = "Your input: $email",
    )
