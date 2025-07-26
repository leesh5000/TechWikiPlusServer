package me.helloc.techwikiplus.user.integration.http

import com.fasterxml.jackson.databind.ObjectMapper
import me.helloc.techwikiplus.user.infrastructure.config.TestContainerConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Controller 통합 테스트를 위한 베이스 클래스
 *
 * TestRestTemplate을 사용하여 실제 HTTP 요청/응답을 테스트
 * 랜덤 포트로 실제 서버를 띄워서 End-to-End 테스트 수행
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Import(TestContainerConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class ControllerIntegrationTestSupport {
    companion object {
        // Spring의 동적 프로퍼티 설정 - 컨테이너의 실제 포트를 애플리케이션에 주입
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            // MySQL 연결 정보 설정
            registry.add("spring.datasource.url") { TestContainerConfig.mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { TestContainerConfig.mysqlContainer.username }
            registry.add("spring.datasource.password") { TestContainerConfig.mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }

            // JPA 설정 - 테스트 시 테이블 자동 생성
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            // Hibernate가 자동으로 dialect를 감지하므로 명시적 지정 불필요

            // Redis 연결 정보 설정
            registry.add("spring.data.redis.host") { TestContainerConfig.redisContainer.host }
            registry.add("spring.data.redis.port") { TestContainerConfig.redisContainer.getMappedPort(6379) }
        }
    }

    @Autowired
    protected lateinit var restTemplate: TestRestTemplate

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    /**
     * JSON 요청을 위한 HttpEntity 생성
     */
    protected fun <T> createJsonHttpEntity(body: T): HttpEntity<T> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, headers)
    }

    /**
     * Authorization 헤더가 포함된 HttpEntity 생성
     */
    protected fun <T> createJsonHttpEntityWithAuth(
        body: T?,
        token: String,
    ): HttpEntity<T> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBearerAuth(token)
        return HttpEntity(body, headers)
    }

    /**
     * 에러 응답 본문 파싱
     */
    protected fun parseErrorResponse(responseBody: String): ErrorResponse {
        return objectMapper.readValue(responseBody, ErrorResponse::class.java)
    }

    data class ErrorResponse(
        val errorCode: String,
        val message: String,
        val timestamp: String,
        val path: String,
        val localizedMessage: String? = null,
        val details: Map<String, Any>? = null,
    )
}
