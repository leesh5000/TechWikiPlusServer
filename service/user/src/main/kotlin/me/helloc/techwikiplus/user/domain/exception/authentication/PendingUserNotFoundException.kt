package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class PendingUserNotFoundException(
    val email: String,
) : DomainException(
        errorCode = ErrorCode.PENDING_USER_NOT_FOUND,
        details =
            "Pending user not found for email: $email. " +
                "Please ensure you have registered and requested verification.",
    )
