package me.helloc.techwikiplus.service.document.domain.model

import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode

/**
 * Title 정책
 * - 문서의 제목을 나타내는 값 객체
 * - 공백을 허용하지만, 앞뒤 공백은 제거하여 저장
 * - 특수문자는 일부 허용 (기술 문서에서 자주 사용되는 문자들)
 */
class Title(value: String) {
    val value: String = value.trim()

    init {
        if (this.value.isBlank()) {
            throw DocumentDomainException(
                documentErrorCode = DocumentErrorCode.BLANK_TITLE,
                params = arrayOf("title"),
            )
        }
        if (this.value.length < MIN_LENGTH) {
            throw DocumentDomainException(
                documentErrorCode = DocumentErrorCode.TITLE_TOO_SHORT,
                params = arrayOf<Any>("title", MIN_LENGTH),
            )
        }
        if (this.value.length > MAX_LENGTH) {
            throw DocumentDomainException(
                documentErrorCode = DocumentErrorCode.TITLE_TOO_LONG,
                params = arrayOf<Any>("title", MAX_LENGTH),
            )
        }
        if (!this.value.matches(ALLOWED_PATTERN)) {
            throw DocumentDomainException(
                documentErrorCode = DocumentErrorCode.TITLE_CONTAINS_INVALID_CHAR,
                params = arrayOf("title"),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Title) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "Title(value=$value)"
    }

    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 200

        // 한글, 영문, 숫자, 공백, 일부 특수문자 허용 (기술 문서에서 자주 사용되는 문자들)
        // 주의: 공백은 일반 스페이스만 허용, 제어 문자(탭, 개행 등)는 허용하지 않음
        private val ALLOWED_PATTERN = """^[가-힣a-zA-Z0-9 \-_.,():/@#&+\[\]{}'"]+$""".toRegex()
    }
}
