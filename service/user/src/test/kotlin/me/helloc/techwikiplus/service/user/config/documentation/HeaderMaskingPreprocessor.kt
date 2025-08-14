package me.helloc.techwikiplus.service.user.config.documentation

import org.springframework.http.HttpHeaders
import org.springframework.restdocs.operation.OperationRequest
import org.springframework.restdocs.operation.OperationRequestFactory
import org.springframework.restdocs.operation.OperationResponse
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor

/**
 * RestDocs 문서 생성 시 헤더의 민감한 데이터를 마스킹하는 Preprocessor
 *
 * Authorization 헤더의 JWT 토큰을 플레이스홀더로 교체합니다.
 */
class HeaderMaskingPreprocessor : OperationPreprocessor {
    private val requestFactory = OperationRequestFactory()

    companion object {
        // JWT 토큰 패턴
        private val JWT_PATTERN = Regex("""Bearer\s+eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")
    }

    override fun preprocess(request: OperationRequest): OperationRequest {
        val headers = request.headers
        val authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION)

        if (authorizationHeader != null && JWT_PATTERN.matches(authorizationHeader)) {
            // Authorization 헤더를 플레이스홀더로 교체
            val maskedHeaders = HttpHeaders()
            headers.forEach { (key, values) ->
                if (key == HttpHeaders.AUTHORIZATION) {
                    maskedHeaders.add(key, "Bearer <JWT_TOKEN>")
                } else {
                    values.forEach { value ->
                        maskedHeaders.add(key, value)
                    }
                }
            }

            return requestFactory.create(
                request.uri,
                request.method,
                request.content,
                maskedHeaders,
                request.parts,
                request.cookies,
            )
        }

        return request
    }

    override fun preprocess(response: OperationResponse): OperationResponse {
        // Response는 그대로 반환
        return response
    }
}

/**
 * HeaderMaskingPreprocessor 인스턴스를 생성하는 헬퍼 함수
 */
fun maskHeaders(): HeaderMaskingPreprocessor {
    return HeaderMaskingPreprocessor()
}
