package me.helloc.techwikiplus.service.user.domain.exception

import me.helloc.techwikiplus.service.user.domain.model.value.Email

class MailSendException(
    email: Email,
    cause: Throwable? = null,
) : UserDomainException("이메일(${email.value}) 전송에 실패했습니다.", cause)
