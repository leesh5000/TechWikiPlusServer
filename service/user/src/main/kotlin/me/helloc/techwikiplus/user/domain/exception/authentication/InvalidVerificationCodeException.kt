package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class InvalidVerificationCodeException(
    val invalidCode: String,
) : DomainException(
        errorCode = ErrorCode.INVALID_VERIFICATION_CODE,
        details = "Invalid verification code: $invalidCode. Please check the code and try again.",
    )
