package me.helloc.techwikiplus.service.document.domain.exception

enum class DocumentErrorCode {
    // Title Validation
    BLANK_TITLE,
    TITLE_TOO_SHORT,
    TITLE_TOO_LONG,
    TITLE_CONTAINS_INVALID_CHAR,
}
