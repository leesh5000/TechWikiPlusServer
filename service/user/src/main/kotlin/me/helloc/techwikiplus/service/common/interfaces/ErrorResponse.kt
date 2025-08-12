package me.helloc.techwikiplus.service.common.interfaces

import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    // ISO-8601 표준
    val timestamp: String = Instant.now().toString(),
) {
    companion object {
        private const val MAX_MESSAGE_LENGTH = 500
        private const val MAX_CODE_LENGTH = 50

        fun of(
            code: String,
            message: String,
        ): ErrorResponse {
            return ErrorResponse(
                code = sanitizeCode(code),
                message = message.take(MAX_MESSAGE_LENGTH),
                timestamp = Instant.now().toString(),
            )
        }

        private fun sanitizeCode(code: String): String {
            // 에러 코드는 영문 대문자, 숫자, 언더스코어만 허용
            return code.replace(Regex("[^A-Z0-9_]"), "")
                .take(MAX_CODE_LENGTH)
                .ifEmpty { "UNKNOWN_ERROR" }
        }
    }
}
