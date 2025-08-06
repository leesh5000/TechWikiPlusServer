package me.helloc.techwikiplus.service.user.infrastructure.security

import org.apache.commons.text.StringEscapeUtils
import org.springframework.stereotype.Component

@Component
class InputSanitizer {
    companion object {
        private const val MAX_URI_LENGTH = 2000
        private const val MAX_MESSAGE_LENGTH = 500
        private const val MAX_FIELD_LENGTH = 100
        private val DANGEROUS_PATTERNS =
            listOf(
                "<script", "</script", "javascript:", "onerror=", "onload=",
                "onclick=", "onmouseover=", "<iframe", "<object", "<embed",
                "<svg", "onmouseenter=", "onfocus=", "onblur=", "<img",
                "alert(", "prompt(", "confirm(", "document.", "window.",
                "eval(", "setTimeout(", "setInterval(", "Function(",
                "<style", "</style", "<link", "<meta", "innerHTML",
                ".cookie", "localStorage", "sessionStorage",
            )
        private val CONTROL_CHAR_REGEX = Regex("[\\x00-\\x1F\\x7F]")
    }

    fun sanitizeUri(uri: String?): String? {
        return uri?.let { sanitizeInput(it, MAX_URI_LENGTH) }
    }

    fun sanitizeMessage(message: String?): String? {
        return message?.let { sanitizeInput(it, MAX_MESSAGE_LENGTH) }
    }

    fun sanitizeFieldName(fieldName: String?): String? {
        return fieldName?.let { sanitizeInput(it, MAX_FIELD_LENGTH) }
    }

    fun sanitizeDetails(details: Map<String, Any>?): Map<String, Any>? {
        return details?.mapValues { entry ->
            when (val value = entry.value) {
                is String -> sanitizeInput(value, MAX_FIELD_LENGTH) ?: ""
                else -> value
            }
        }
    }

    private fun sanitizeInput(
        input: String,
        maxLength: Int,
    ): String? {
        if (input.isEmpty()) return input

        // 1. 길이 제한
        val truncated =
            if (input.length > maxLength) {
                input.take(maxLength)
            } else {
                input
            }

        // 2. 제어 문자 제거
        val withoutControlChars = truncated.replace(CONTROL_CHAR_REGEX, "")

        // 3. 위험 패턴 검사 및 HTML 이스케이프
        return if (containsDangerousPattern(withoutControlChars)) {
            StringEscapeUtils.escapeHtml4(withoutControlChars)
        } else {
            // 기본적인 HTML 특수문자는 항상 이스케이프
            escapeBasicHtml(withoutControlChars)
        }
    }

    private fun containsDangerousPattern(input: String): Boolean {
        val lowerInput = input.lowercase()
        return DANGEROUS_PATTERNS.any { pattern ->
            lowerInput.contains(pattern.lowercase())
        }
    }

    private fun escapeBasicHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }
}