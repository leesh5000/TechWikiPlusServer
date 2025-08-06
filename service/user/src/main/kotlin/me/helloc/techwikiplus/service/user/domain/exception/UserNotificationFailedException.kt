package me.helloc.techwikiplus.service.user.domain.exception

class UserNotificationFailedException(
    message: String,
    cause: Throwable? = null,
) : UserDomainException(message, cause) {
    constructor(cause: Throwable) : this("사용자 알림에 실패했습니다.", cause)
    constructor(message: String) : this(message, null)
}
