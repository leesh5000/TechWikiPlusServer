package me.helloc.techwikiplus.user.domain.exception.notfound

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class UserEmailNotFoundException(
    val email: String,
) : DomainException(
        errorCode = ErrorCode.USER_NOT_FOUND,
        details = "User not found with email: $email",
    )
