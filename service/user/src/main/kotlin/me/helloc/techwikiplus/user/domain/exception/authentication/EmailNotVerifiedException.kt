package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class EmailNotVerifiedException : DomainException(
    errorCode = ErrorCode.EMAIL_NOT_VERIFIED,
    details = "Please verify your email before logging in.",
)
