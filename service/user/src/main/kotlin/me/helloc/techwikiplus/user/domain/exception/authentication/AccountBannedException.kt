package me.helloc.techwikiplus.user.domain.exception.authentication

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class AccountBannedException : DomainException(
    errorCode = ErrorCode.ACCOUNT_BANNED,
    details = "Your account has been banned.",
)
