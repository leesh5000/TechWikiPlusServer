package me.helloc.techwikiplus.service.user.adapter.outbound.mail

class MailSendingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
