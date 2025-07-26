package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class ExpiredEmailVerificationException(
    val email: String,
) : DomainException(
        errorCode = ErrorCode.EXPIRED_EMAIL_VERIFICATION,
        details = "Email verification expired for email: $email. Please request a new verification code.",
    )
