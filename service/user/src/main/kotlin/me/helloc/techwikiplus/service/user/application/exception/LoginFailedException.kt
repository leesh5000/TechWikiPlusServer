package me.helloc.techwikiplus.service.user.application.exception

class LoginFailedException(
    message: String,
    cause: Throwable? = null,
) : ApplicationException(message, cause) {
    constructor(cause: Throwable) : this("로그인에 실패했습니다", cause)
}
