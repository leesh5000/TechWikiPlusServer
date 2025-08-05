package me.helloc.techwikiplus.service.user.adapter.outbound.test.config.documentation

import com.epages.restdocs.apispec.ResourceSnippetParameters
import org.springframework.test.web.servlet.ResultHandler

/**
 * API 문서화를 지원하는 인터페이스
 *
 * 통합 테스트에서 선택적으로 API 문서를 생성할 수 있도록 지원합니다.
 * 이 인터페이스를 구현하는 클래스는 REST Docs를 사용하여
 * API 문서를 자동으로 생성하는 기능을 제공합니다.
 */
interface ApiDocumentationSupport {
    /**
     * API 문서화를 위한 ResultHandler를 생성합니다.
     *
     * @param identifier 문서 식별자 (생성될 문서 파일명)
     * @param resourceParameters API 리소스 정보 (태그, 설명, 요청/응답 필드 등)
     * @return MockMvc 테스트에서 사용할 ResultHandler
     */
    fun documentWithResource(
        identifier: String,
        resourceParameters: ResourceSnippetParameters,
    ): ResultHandler

    /**
     * 문서화 기능이 활성화되어 있는지 확인합니다.
     *
     * @return 문서화가 활성화되어 있으면 true, 아니면 false
     */
    fun isDocumentationEnabled(): Boolean
}
