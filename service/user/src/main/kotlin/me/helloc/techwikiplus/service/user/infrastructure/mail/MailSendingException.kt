package me.helloc.techwikiplus.service.user.infrastructure.mail

class MailSendingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
