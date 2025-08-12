package me.helloc.techwikiplus.service.config.documentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.springframework.http.MediaType
import org.springframework.restdocs.operation.OperationRequest
import org.springframework.restdocs.operation.OperationResponse
import org.springframework.restdocs.operation.OperationResponseFactory
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor

/**
 * RestDocs 문서 생성 시 민감한 데이터를 마스킹하는 Preprocessor
 *
 * JWT 토큰, 사용자 ID, 타임스탬프 등을 플레이스홀더로 교체합니다.
 */
class SensitiveDataMaskingPreprocessor : OperationPreprocessor {
    private val objectMapper = ObjectMapper()
    private val responseFactory = OperationResponseFactory()

    companion object {
        // JWT 토큰 패턴 (Base64 인코딩된 3개 파트)
        private val JWT_PATTERN = Regex("""eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")

        // Snowflake ID 패턴 (16자리 이상 숫자)
        private val SNOWFLAKE_ID_PATTERN = Regex("""\d{16,}""")

        // ISO 8601 타임스탬프 패턴
        private val ISO_TIMESTAMP_PATTERN = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?Z?""")
    }

    override fun preprocess(request: OperationRequest): OperationRequest {
        // Request는 그대로 반환
        return request
    }

    override fun preprocess(response: OperationResponse): OperationResponse {
        // JSON 응답만 처리
        if (!isJsonResponse(response)) {
            return response
        }

        val originalContent = response.content
        if (originalContent.isEmpty()) {
            return response
        }

        try {
            val contentString = String(originalContent)
            val rootNode = objectMapper.readTree(contentString)

            if (rootNode is ObjectNode) {
                maskSensitiveFields(rootNode)
            }

            val maskedContent = objectMapper.writeValueAsBytes(rootNode)

            return responseFactory.create(
                response.status,
                response.headers,
                maskedContent,
            )
        } catch (e: Exception) {
            // 파싱 실패 시 원본 반환
            return response
        }
    }

    private fun isJsonResponse(response: OperationResponse): Boolean {
        val contentType = response.headers.getFirst("Content-Type")
        return contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)
    }

    private fun maskSensitiveFields(node: ObjectNode) {
        // accessToken 필드 마스킹
        if (node.has("accessToken")) {
            node.set<TextNode>("accessToken", TextNode("<ACCESS_TOKEN>"))
        }

        // refreshToken 필드 마스킹
        if (node.has("refreshToken")) {
            node.set<TextNode>("refreshToken", TextNode("<REFRESH_TOKEN>"))
        }

        // userId 필드 마스킹 (Snowflake ID인 경우)
        if (node.has("userId")) {
            val userIdValue = node.get("userId").asText()
            if (SNOWFLAKE_ID_PATTERN.matches(userIdValue)) {
                node.set<TextNode>("userId", TextNode("<USER_ID>"))
            }
        }

        // 타임스탬프 필드 마스킹
        if (node.has("accessTokenExpiresAt")) {
            node.set<TextNode>("accessTokenExpiresAt", TextNode("<ISO_TIMESTAMP>"))
        }

        if (node.has("refreshTokenExpiresAt")) {
            node.set<TextNode>("refreshTokenExpiresAt", TextNode("<ISO_TIMESTAMP>"))
        }

        // timestamp 필드 마스킹 (에러 응답)
        if (node.has("timestamp")) {
            val timestampValue = node.get("timestamp").asText()
            if (ISO_TIMESTAMP_PATTERN.matches(timestampValue)) {
                node.set<TextNode>("timestamp", TextNode("<ISO_TIMESTAMP>"))
            }
        }

        // createdAt, modifiedAt 필드 마스킹
        if (node.has("createdAt")) {
            val createdAtValue = node.get("createdAt").asText()
            if (ISO_TIMESTAMP_PATTERN.matches(createdAtValue)) {
                node.set<TextNode>("createdAt", TextNode("<ISO_TIMESTAMP>"))
            }
        }

        if (node.has("modifiedAt")) {
            val modifiedAtValue = node.get("modifiedAt").asText()
            if (ISO_TIMESTAMP_PATTERN.matches(modifiedAtValue)) {
                node.set<TextNode>("modifiedAt", TextNode("<ISO_TIMESTAMP>"))
            }
        }

        // 중첩된 객체 처리
        node.fields().forEach { entry ->
            val value = entry.value
            if (value is ObjectNode) {
                maskSensitiveFields(value)
            }
        }
    }
}

/**
 * SensitiveDataMaskingPreprocessor 인스턴스를 생성하는 헬퍼 함수
 */
fun maskSensitiveData(): SensitiveDataMaskingPreprocessor {
    return SensitiveDataMaskingPreprocessor()
}
