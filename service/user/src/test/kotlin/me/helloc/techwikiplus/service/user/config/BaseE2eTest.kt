package me.helloc.techwikiplus.service.user.config

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.epages.restdocs.apispec.ResourceDocumentation
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.fasterxml.jackson.databind.ObjectMapper
import me.helloc.techwikiplus.service.user.config.config.TestContainersInitializer
import me.helloc.techwikiplus.service.user.config.documentation.ApiDocumentationSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
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
 * - Redis 캐시 초기화로 테스트 간 완전한 격리 보장
 * - 실제 운영 환경과 유사한 테스트 환경
 * - 선택적 API 문서화 지원
 *
 * 테스트 격리 전략:
 * - MySQL: @Transactional 어노테이션으로 각 테스트 후 자동 롤백
 * - Redis: clearRedisCache()로 각 테스트 전 수동 초기화
 */
@ExtendWith(SpringExtension::class, RestDocumentationExtension::class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e-test")
@ContextConfiguration(initializers = [TestContainersInitializer::class])
@Transactional
abstract class BaseE2eTest : ApiDocumentationSupport {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @Value("\${api.documentation.enabled:false}")
    private var documentationEnabled: Boolean = false

    private var restDocumentation: RestDocumentationContextProvider? = null

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider?) {
        this.restDocumentation = restDocumentation

        // 각 테스트 전에 Redis 캐시 초기화
        clearRedisCache()

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

    /**
     * Redis 캐시를 초기화하여 테스트 간 격리를 보장
     *
     * 참고: MySQL은 @Transactional 어노테이션 덕분에 각 테스트 후 자동으로 롤백되므로
     * 별도의 초기화가 필요 없습니다. 하지만 Redis는 트랜잭션 범위 밖에 있어서
     * 수동으로 초기화해야 합니다.
     */
    private fun clearRedisCache() {
        stringRedisTemplate.connectionFactory?.connection?.flushAll()
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
