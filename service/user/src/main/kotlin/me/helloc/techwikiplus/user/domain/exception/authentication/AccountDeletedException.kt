package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class AccountDeletedException : DomainException(
    errorCode = ErrorCode.ACCOUNT_DELETED,
    details = "Your account has been deleted.",
)
