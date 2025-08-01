package me.helloc.techwikiplus.service.user.test

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.epages.restdocs.apispec.ResourceDocumentation
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.fasterxml.jackson.databind.ObjectMapper
import me.helloc.techwikiplus.test.config.TestContainersInitializer
import me.helloc.techwikiplus.test.documentation.ApiDocumentationSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultHandler
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

/**
 * 통합 테스트를 위한 기본 클래스
 *
 * - TestContainers를 사용한 실제 MySQL 연동
 * - 전체 애플리케이션 컨텍스트 로드
 * - 트랜잭션 롤백으로 테스트 격리
 * - 실제 운영 환경과 유사한 테스트 환경
 * - 선택적 API 문서화 지원
 */
@ExtendWith(SpringExtension::class, RestDocumentationExtension::class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@ContextConfiguration(initializers = [TestContainersInitializer::class])
@Transactional
abstract class BaseIntegrationTest : ApiDocumentationSupport {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var context: WebApplicationContext

    @Value("\${api.documentation.enabled:false}")
    private var documentationEnabled: Boolean = false

    private var restDocumentation: RestDocumentationContextProvider? = null

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider?) {
        this.restDocumentation = restDocumentation

        if (documentationEnabled && restDocumentation != null) {
            // 문서화가 활성화된 경우 REST Docs 설정
            this.mockMvc =
                MockMvcBuilders.webAppContextSetup(context)
                    .apply<DefaultMockMvcBuilder>(
                        documentationConfiguration(restDocumentation)
                            .operationPreprocessors()
                            .withRequestDefaults(prettyPrint())
                            .withResponseDefaults(prettyPrint()),
                    )
                    .build()
        }
    }

    override fun documentWithResource(
        identifier: String,
        resourceParameters: ResourceSnippetParameters,
    ): ResultHandler {
        return if (documentationEnabled) {
            document(
                identifier,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                ResourceDocumentation.resource(resourceParameters),
            )
        } else {
            // 문서화가 비활성화된 경우 아무것도 하지 않는 ResultHandler 반환
            ResultHandler { }
        }
    }

    override fun isDocumentationEnabled(): Boolean = documentationEnabled
}
