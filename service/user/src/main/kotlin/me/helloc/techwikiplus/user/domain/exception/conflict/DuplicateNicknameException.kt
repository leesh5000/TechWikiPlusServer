package me.helloc.techwikiplus.user.domain.exception.conflict

import me.helloc.techwikiplus.user.domain.exception.DomainException
import me.helloc.techwikiplus.user.domain.exception.ErrorCode

class DuplicateNicknameException(
    nickname: String,
) : DomainException(
        errorCode = ErrorCode.DUPLICATE_NICKNAME,
        details = "Your input: $nickname",
    )
