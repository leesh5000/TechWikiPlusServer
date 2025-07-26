package me.helloc.techwikiplus.user.domain.exception.validation

import me.helloc.techwikiplus.user.domain.DomainConstants
import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class InvalidPasswordException(
    password: String,
) : DomainException(
        errorCode = ErrorCode.INVALID_PASSWORD,
        details =
            "Password must be ${DomainConstants.Password.MIN_LENGTH}-" +
                "${DomainConstants.Password.MAX_LENGTH} characters long and include uppercase, " +
                "lowercase, numbers, and special characters. Your input: $password",
    )
