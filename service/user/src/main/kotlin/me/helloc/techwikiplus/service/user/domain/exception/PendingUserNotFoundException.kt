package me.helloc.techwikiplus.service.user.domain.exception

import me.helloc.techwikiplus.service.user.domain.model.value.Email

class PendingUserNotFoundException(
    email: Email,
) : UserDomainException(
        "회원 가입 대기중인 사용자(${email.value})를 찾을 수 없습니다.",
    )
