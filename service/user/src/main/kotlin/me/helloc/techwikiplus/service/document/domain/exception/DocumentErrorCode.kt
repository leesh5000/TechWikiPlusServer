package me.helloc.techwikiplus.service.document.domain.exception

enum class DocumentErrorCode {
    // DocumentId Validation
    INVALID_DOCUMENT_ID_FORMAT,

    // Title Validation
    BLANK_TITLE,
    TITLE_TOO_SHORT,
    TITLE_TOO_LONG,
    TITLE_CONTAINS_INVALID_CHAR,

    // Content Validation
    BLANK_CONTENT,
    CONTENT_TOO_SHORT,
    CONTENT_TOO_LONG,
}
