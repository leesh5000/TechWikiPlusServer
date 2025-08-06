package me.helloc.techwikiplus.service.user.application.exception

open class ApplicationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
