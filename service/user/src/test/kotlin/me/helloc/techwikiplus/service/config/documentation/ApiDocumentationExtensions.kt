package me.helloc.techwikiplus.service.config.documentation

import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder
import org.springframework.restdocs.payload.FieldDescriptor

/**
 * * API 문서화를 위한 확장 함수 모음
 *  *
 *  * ResourceSnippetParametersBuilder에 대한 편리한 확장 함수를 제공하여
 *  * 반복되는 코드를 줄이고 테스트 코드의 가독성을 향상시킵니다.
 *
 *  ---
 *
 * 표준 에러 응답 필드와 스키마를 설정합니다.
 *
 * ErrorResponse 클래스의 기본 필드(code, message, timestamp)를 자동으로 설정합니다.
 *
 * @return 빌더 체인을 위한 ResourceSnippetParametersBuilder 인스턴스
 */
fun ResourceSnippetParametersBuilder.withStandardErrorResponse(): ResourceSnippetParametersBuilder {
    this.responseFields(*ErrorResponseDocumentation.standardFields)
    this.responseSchema(ErrorResponseDocumentation.standardSchema)
    return this
}

/**
 * 커스텀 필드와 함께 에러 응답 필드를 설정합니다.
 *
 * 표준 에러 필드에 추가로 커스텀 필드를 포함하는 경우 사용합니다.
 *
 * @param additionalFields 추가할 커스텀 필드들
 * @return 빌더 체인을 위한 ResourceSnippetParametersBuilder 인스턴스
 */
fun ResourceSnippetParametersBuilder.withErrorResponseFields(
    vararg additionalFields: FieldDescriptor,
): ResourceSnippetParametersBuilder {
    val allFields = ErrorResponseDocumentation.withAdditionalFields(*additionalFields)
    this.responseFields(*allFields)
    this.responseSchema(ErrorResponseDocumentation.standardSchema)
    return this
}

/**
 * 선택적으로 에러 응답 필드를 설정합니다.
 *
 * 특정 필드만 문서화하고 싶을 때 사용합니다.
 *
 * @param fields 포함할 필드 이름들 (예: "code", "message")
 * @return 빌더 체인을 위한 ResourceSnippetParametersBuilder 인스턴스
 */
fun ResourceSnippetParametersBuilder.withSelectedErrorFields(vararg fields: String): ResourceSnippetParametersBuilder {
    val selectedFields = ErrorResponseDocumentation.selectFields(*fields)
    this.responseFields(*selectedFields)
    this.responseSchema(ErrorResponseDocumentation.standardSchema)
    return this
}

/**
 * 에러 응답 문서를 빠르게 생성하는 헬퍼 함수
 *
 * 가장 일반적인 에러 응답 케이스를 위한 단축 함수입니다.
 * 태그, 요약, 설명을 설정하고 표준 에러 응답 필드를 자동으로 추가합니다.
 *
 * @param tag API 태그
 * @param summary API 요약
 * @param description API 설명
 * @return 빌더 체인을 위한 ResourceSnippetParametersBuilder 인스턴스
 */
fun ResourceSnippetParametersBuilder.buildErrorResponse(
    tag: String,
    summary: String,
    description: String,
): ResourceSnippetParametersBuilder {
    return this
        .tag(tag)
        .summary(summary)
        .description(description)
        .withStandardErrorResponse()
}
