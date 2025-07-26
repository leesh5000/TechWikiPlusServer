package me.helloc.techwikiplus.user.domain.exception.validation

import me.helloc.techwikiplus.user.domain.DomainConstants
import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class InvalidNicknameException(
    val nickname: String,
) : DomainException(
        errorCode = ErrorCode.INVALID_NICKNAME,
        details =
            "Nickname must be ${DomainConstants.Nickname.MIN_LENGTH}-" +
                "${DomainConstants.Nickname.MAX_LENGTH} characters long and can only contain " +
                "alphanumeric characters and Korean characters. Your input: $nickname",
    )
