package me.helloc.techwikiplus.documentation

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.epages.restdocs.apispec.ResourceDocumentation
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.fasterxml.jackson.databind.ObjectMapper
import me.helloc.techwikiplus.test.config.TestContainersInitializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

/**
 * API 문서화를 위한 기본 테스트 클래스
 *
 * 이 클래스는 REST Docs와 restdocs-api-spec을 사용하여
 * API 문서를 자동으로 생성하는 테스트의 기반 클래스입니다.
 * TestContainers를 통해 실제 DB 환경에서 문서를 생성합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension::class)
@ActiveProfiles("integration-test")
@ContextConfiguration(initializers = [TestContainersInitializer::class])
@Transactional
abstract class ApiDocumentationTest {
    /**
     * 테스트 클래스에서 필드 주입 사용 이유:
     * - Spring 테스트 프레임워크의 제약으로 인해 테스트 클래스는 생성자 주입을 지원하지 않음
     * - MockMvc와 같은 테스트 전용 빈은 Spring의 테스트 인프라에서 특별히 관리됨
     * - 프로덕션 코드와 달리 테스트 코드에서는 필드 주입이 일반적이고 허용되는 패턴
     */
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var context: WebApplicationContext

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
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

    /**
     * 문서화를 위한 헬퍼 메서드
     *
     * @param identifier 문서 식별자
     * @param resourceParameters 리소스 파라미터
     * @return 문서화 설정이 적용된 ResultHandler
     */
    protected fun documentWithResource(
        identifier: String,
        resourceParameters: ResourceSnippetParameters,
    ) = document(
        identifier,
        preprocessRequest(prettyPrint()),
        preprocessResponse(prettyPrint()),
        ResourceDocumentation.resource(resourceParameters),
    )
}
