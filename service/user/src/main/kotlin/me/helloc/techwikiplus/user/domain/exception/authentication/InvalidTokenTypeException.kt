package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class InvalidTokenTypeException : DomainException(
    errorCode = ErrorCode.INVALID_TOKEN_TYPE,
    details = "Expected refresh token but received access token",
)
