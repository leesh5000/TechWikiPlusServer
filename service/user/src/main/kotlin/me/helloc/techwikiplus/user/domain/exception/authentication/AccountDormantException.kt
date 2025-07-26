package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class AccountDormantException : DomainException(
    errorCode = ErrorCode.ACCOUNT_DORMANT,
    details = "Your account is dormant. Please contact support to reactivate.",
)
