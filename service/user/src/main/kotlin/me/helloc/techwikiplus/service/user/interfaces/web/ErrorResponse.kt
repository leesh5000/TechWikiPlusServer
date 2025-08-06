package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.user.infrastructure.security.InputSanitizer
import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, Any>? = null,
) {
    companion object {
        private val sanitizer = InputSanitizer()

        fun of(
            code: String,
            message: String,
        ): ErrorResponse {
            return ErrorResponse(
                code = sanitizeCode(code),
                message = sanitizer.sanitizeMessage(message) ?: "An error occurred",
            )
        }

        private fun sanitizeCode(code: String): String {
            // 에러 코드는 영문 대문자, 숫자, 언더스코어만 허용
            return code.replace(Regex("[^A-Z0-9_]"), "")
                .take(50)
                .ifEmpty { "UNKNOWN_ERROR" }
        }
    }
}