package me.helloc.techwikiplus.service.document.domain.exception

open class DocumentDomainException(
    val documentErrorCode: DocumentErrorCode,
    val params: Array<out Any?> = emptyArray(),
    cause: Throwable? = null,
) : RuntimeException(documentErrorCode.name, cause)