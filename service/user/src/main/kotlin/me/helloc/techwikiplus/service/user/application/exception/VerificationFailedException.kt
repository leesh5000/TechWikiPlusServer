package me.helloc.techwikiplus.service.user.application.exception

class VerificationFailedException(
    message: String,
    cause: Throwable? = null,
) : ApplicationException(message, cause) {
    constructor(cause: Throwable) : this("이메일 인증에 실패했습니다", cause)
}
