package me.helloc.techwikiplus.service.user.domain.exception

import me.helloc.techwikiplus.service.user.domain.model.value.Email

class RegistrationCodeMismatchException(email: Email) :
    UserDomainException(
        "이메일 ${email.value}에 대한 회원 가입 코드가 일치하지 않습니다. " +
            "이메일을 다시 확인하고 시도해주세요.",
    )
