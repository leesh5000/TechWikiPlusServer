package me.helloc.techwikiplus.service.user.interfaces.web.dto

import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val path: String? = null,
    val details: Map<String, Any>? = null,
) {
    companion object {
        fun of(
            code: String,
            message: String,
            path: String? = null,
        ): ErrorResponse {
            return ErrorResponse(
                code = code,
                message = message,
                path = path,
            )
        }

        fun validation(
            message: String,
            path: String? = null,
            details: Map<String, Any>? = null,
        ): ErrorResponse {
            return ErrorResponse(
                code = "VALIDATION_ERROR",
                message = message,
                path = path,
                details = details,
            )
        }
    }
}
