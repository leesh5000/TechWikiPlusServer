package me.helloc.techwikiplus.service.document.interfaces.web

import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class DocumentErrorCodeMapper {
    fun mapToHttpStatus(documentErrorCode: DocumentErrorCode): HttpStatus {
        return when (documentErrorCode) {
            // Title Validation
            DocumentErrorCode.BLANK_TITLE,
            DocumentErrorCode.TITLE_TOO_SHORT,
            DocumentErrorCode.TITLE_TOO_LONG,
            DocumentErrorCode.TITLE_CONTAINS_INVALID_CHAR,
            -> HttpStatus.BAD_REQUEST
        }
    }

    fun mapToMessage(
        documentErrorCode: DocumentErrorCode,
        params: Array<out Any?>,
    ): String {
        val baseMessage =
            when (documentErrorCode) {
                // Title Validation
                DocumentErrorCode.BLANK_TITLE -> "제목은 필수 입력 항목입니다"
                DocumentErrorCode.TITLE_TOO_SHORT ->
                    if (params.size > 1) {
                        "제목은 최소 ${params[1]}자 이상이어야 합니다"
                    } else {
                        "제목이 너무 짧습니다"
                    }
                DocumentErrorCode.TITLE_TOO_LONG ->
                    if (params.size > 1) {
                        "제목은 최대 ${params[1]}자 이하여야 합니다"
                    } else {
                        "제목이 너무 깁니다"
                    }
                DocumentErrorCode.TITLE_CONTAINS_INVALID_CHAR ->
                    "제목에 사용할 수 없는 문자가 포함되어 있습니다. " +
                        "한글, 영문, 숫자, 공백 및 일부 특수문자(-_.,():/@#&+[]{}'\")만 사용 가능합니다"
            }

        return baseMessage
    }
}
