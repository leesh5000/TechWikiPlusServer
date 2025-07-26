package me.helloc.techwikiplus.user.domain.exception.notfound

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class ResourceNotFoundException(
    resource: String,
) : DomainException(
        errorCode = ErrorCode.RESOURCE_NOT_FOUND,
        details = "Resource not found: $resource",
    )
