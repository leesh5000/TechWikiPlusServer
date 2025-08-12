package me.helloc.techwikiplus.service.document.domain.model

import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode

class DocumentId(val value: Long) {
    init {
        if (value <= 0) {
            throw DocumentDomainException(
                documentErrorCode = DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT,
                params = arrayOf("documentId"),
            )
        }
        // Snowflake ID는 64비트 정수이므로 Long 타입의 최대값을 넘지 않음
        // 추가 검증이 필요하면 여기에 추가
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentId) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    companion object {
        fun from(value: String): DocumentId {
            try {
                return DocumentId(value.toLong())
            } catch (e: NumberFormatException) {
                throw DocumentDomainException(
                    documentErrorCode = DocumentErrorCode.INVALID_DOCUMENT_ID_FORMAT,
                    params = arrayOf(value),
                    cause = e,
                )
            }
        }
    }
}
