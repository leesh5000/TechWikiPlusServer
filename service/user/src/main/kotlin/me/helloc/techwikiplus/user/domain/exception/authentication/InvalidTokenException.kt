package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class InvalidTokenException : DomainException(
    errorCode = ErrorCode.INVALID_TOKEN,
    details = "Invalid refresh token",
)
