package me.helloc.techwikiplus.user.domain.exception.ratelimit

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class ResendRateLimitExceededException(
    message: String,
) : DomainException(
        errorCode = ErrorCode.RATE_LIMIT_EXCEEDED,
        details = message,
    )
