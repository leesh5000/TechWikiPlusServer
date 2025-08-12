package me.helloc.techwikiplus.service.user.domain.exception

open class UserDomainException(
    val userErrorCode: UserErrorCode,
    val params: Array<out Any?> = emptyArray(),
    cause: Throwable? = null,
) : RuntimeException(userErrorCode.name, cause)
