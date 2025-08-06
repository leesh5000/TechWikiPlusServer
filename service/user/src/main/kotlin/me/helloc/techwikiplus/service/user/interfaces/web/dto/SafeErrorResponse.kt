package me.helloc.techwikiplus.service.user.interfaces.web.dto

import me.helloc.techwikiplus.service.user.interfaces.web.security.InputSanitizer
import java.time.Instant

data class SafeErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val path: String? = null,
    val details: Map<String, Any>? = null,
) {
    companion object {
        private val sanitizer = InputSanitizer()

        fun of(
            code: String,
            message: String,
            path: String? = null,
        ): SafeErrorResponse {
            return SafeErrorResponse(
                code = sanitizeCode(code),
                message = sanitizer.sanitizeMessage(message) ?: "An error occurred",
                path = sanitizer.sanitizeUri(path),
            )
        }

        fun validation(
            message: String,
            path: String? = null,
            details: Map<String, Any>? = null,
        ): SafeErrorResponse {
            return SafeErrorResponse(
                code = "VALIDATION_ERROR",
                message = sanitizer.sanitizeMessage(message) ?: "Validation error",
                path = sanitizer.sanitizeUri(path),
                details = sanitizer.sanitizeDetails(details),
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
