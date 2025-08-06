package me.helloc.techwikiplus.service.user.domain.exception

import me.helloc.techwikiplus.service.user.domain.model.value.Email

class RegistrationEmailNotFoundException(email: Email) :
    UserDomainException(
        "이메일 ${email.value}에 대한 회원 가입 코드가 " +
            "존재하지 않습니다. 회원 가입을 먼저 진행해주세요.",
    )
