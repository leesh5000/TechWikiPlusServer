package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class UnauthorizedAccessException(
    resource: String,
) : DomainException(
        errorCode = ErrorCode.UNAUTHORIZED_ACCESS,
        details = "Unauthorized access to resource: $resource",
    )
