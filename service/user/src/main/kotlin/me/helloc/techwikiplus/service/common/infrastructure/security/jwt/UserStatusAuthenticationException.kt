package me.helloc.techwikiplus.service.common.infrastructure.security.jwt

import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import org.springframework.security.core.AuthenticationException

class UserStatusAuthenticationException(
    val errorCode: UserErrorCode,
    message: String,
) : AuthenticationException(message) {
    constructor(errorCode: UserErrorCode) : this(errorCode, errorCode.name)
}
