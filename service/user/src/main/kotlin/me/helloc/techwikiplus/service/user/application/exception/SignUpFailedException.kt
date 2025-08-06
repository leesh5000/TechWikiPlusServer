package me.helloc.techwikiplus.service.user.application.exception

class SignUpFailedException(
    message: String,
    cause: Throwable? = null,
) : ApplicationException(message, cause) {
    constructor(cause: Throwable) : this("회원가입에 실패했습니다", cause)
}
