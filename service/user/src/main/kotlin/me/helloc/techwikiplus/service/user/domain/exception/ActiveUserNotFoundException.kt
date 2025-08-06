package me.helloc.techwikiplus.service.user.domain.exception

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.UserId

class ActiveUserNotFoundException(
    param: String,
) : UserDomainException(
        "활성화된 사용자($param)를 찾을 수 없습니다.",
    ) {
    constructor(userId: UserId) : this(userId.value)
    constructor(email: Email) : this(email.value)
}
