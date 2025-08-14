package me.helloc.techwikiplus.service.document.domain.model

import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode

class Content(value: String) {
    val value: String = value.trim()

    init {
        if (this.value.isBlank()) {
            throw DocumentDomainException(
                documentErrorCode = DocumentErrorCode.BLANK_CONTENT,
                params = arrayOf("content"),
            )
        }
        if (this.value.length < MIN_LENGTH) {
            throw DocumentDomainException(
                documentErrorCode = DocumentErrorCode.CONTENT_TOO_SHORT,
                params = arrayOf<Any>("content", MIN_LENGTH),
            )
        }
        if (this.value.length > MAX_LENGTH) {
            throw DocumentDomainException(
                documentErrorCode = DocumentErrorCode.CONTENT_TOO_LONG,
                params = arrayOf<Any>("content", MAX_LENGTH),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Content) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "Content(value=${value.take(50)}${if (value.length > 50) "..." else ""})"
    }

    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 50000
    }
}
