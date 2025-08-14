package me.helloc.techwikiplus.service.document.domain.model

import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode

class Author(
    val id: Long,
) {
    init {
        if (id <= 0) {
            throw DocumentDomainException(
                documentErrorCode = DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT,
                params = arrayOf("documentId"),
            )
        }
        // Snowflake ID는 64비트 정수이므로 Long 타입의 최대값을 넘지 않음
        // 추가 검증이 필요하면 여기에 추가
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Author) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id.toString()

    companion object {
        fun from(value: String): Author {
            try {
                return Author(value.toLong())
            } catch (e: NumberFormatException) {
                throw DocumentDomainException(
                    documentErrorCode = DocumentErrorCode.INVALID_AUTHOR_ID_FORMAT,
                    params = arrayOf(value),
                    cause = e,
                )
            }
        }
    }
}
