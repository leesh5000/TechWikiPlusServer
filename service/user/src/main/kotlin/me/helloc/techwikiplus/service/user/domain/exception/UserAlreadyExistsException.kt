package me.helloc.techwikiplus.service.user.domain.exception

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname

class UserAlreadyExistsException(
    message: String,
) : UserDomainException(message) {
    constructor(email: Email) : this("이메일 '${email.value}'은(는) 이미 사용 중입니다.")
    constructor(nickname: Nickname) : this("닉네임 '${nickname.value}'은(는) 이미 사용 중입니다.")
}
