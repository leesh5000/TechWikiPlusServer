package me.helloc.techwikiplus.service.user.config.documentation

import com.epages.restdocs.apispec.Schema
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation

/**
 * 에러 응답 문서화를 위한 표준 필드 정의
 *
 * E2E 테스트에서 반복되는 에러 응답 필드 정의를 중앙화하여
 * 코드 중복을 제거하고 일관성을 유지합니다.
 */
object ErrorResponseDocumentation {
    /**
     * 표준 에러 응답 필드 정의
     *
     * ErrorResponse 클래스의 기본 필드들:
     * - code: 에러 코드
     * - message: 에러 메시지
     * - timestamp: 에러 발생 시간 (ISO-8601 형식)
     */
    val standardFields: Array<FieldDescriptor> =
        arrayOf(
            PayloadDocumentation.fieldWithPath("code")
                .type(JsonFieldType.STRING)
                .description("에러 코드"),
            PayloadDocumentation.fieldWithPath("message")
                .type(JsonFieldType.STRING)
                .description("에러 메시지"),
            PayloadDocumentation.fieldWithPath("timestamp")
                .type(JsonFieldType.STRING)
                .description("에러 발생 시간 (ISO-8601 형식)"),
        )

    /**
     * 표준 에러 응답 스키마
     */
    val standardSchema: Schema = Schema.schema("ErrorResponse")

    /**
     * 커스텀 필드와 함께 에러 응답 필드를 생성
     *
     * @param additionalFields 표준 필드에 추가할 커스텀 필드들
     * @return 표준 필드와 커스텀 필드가 결합된 배열
     */
    fun withAdditionalFields(vararg additionalFields: FieldDescriptor): Array<FieldDescriptor> {
        return standardFields + additionalFields
    }

    /**
     * 특정 필드만 포함하는 에러 응답 필드를 생성
     *
     * @param fields 포함할 필드 이름들
     * @return 선택된 필드들만 포함하는 배열
     */
    fun selectFields(vararg fields: String): Array<FieldDescriptor> {
        val fieldSet = fields.toSet()
        val fieldMap =
            mapOf(
                "code" to
                    PayloadDocumentation.fieldWithPath("code")
                        .type(JsonFieldType.STRING)
                        .description("에러 코드"),
                "message" to
                    PayloadDocumentation.fieldWithPath("message")
                        .type(JsonFieldType.STRING)
                        .description("에러 메시지"),
                "timestamp" to
                    PayloadDocumentation.fieldWithPath("timestamp")
                        .type(JsonFieldType.STRING)
                        .description("에러 발생 시간 (ISO-8601 형식)"),
            )

        return fields.mapNotNull { fieldMap[it] }.toTypedArray()
    }
}
