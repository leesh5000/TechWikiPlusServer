package me.helloc.techwikiplus.service.user.domain.exception

open class DomainException(
    val errorCode: ErrorCode,
    val params: Array<out Any?> = emptyArray(),
    cause: Throwable? = null
) : RuntimeException(errorCode.name, cause)