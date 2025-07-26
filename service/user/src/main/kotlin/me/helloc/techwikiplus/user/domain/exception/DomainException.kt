package me.helloc.techwikiplus.user.domain.exception

abstract class DomainException(
    val errorCode: ErrorCode,
    details: String? = null,
) : RuntimeException(
        buildMessage(errorCode, details),
    ) {
    val code: String
        get() = errorCode.code

    companion object {
        private fun buildMessage(
            errorCode: ErrorCode,
            details: String?,
        ): String {
            return if (details != null) {
                "${errorCode.message}. Details: $details"
            } else {
                errorCode.message
            }
        }
    }
}
