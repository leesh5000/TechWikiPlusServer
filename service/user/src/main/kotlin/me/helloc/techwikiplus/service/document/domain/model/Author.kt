package me.helloc.techwikiplus.service.document.domain.model

import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode

class Author(
    val authorId: Long,
) {
    init {
        if (authorId <= 0) {
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
        return authorId == other.authorId
    }

    override fun hashCode(): Int = authorId.hashCode()

    override fun toString(): String = authorId.toString()

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
