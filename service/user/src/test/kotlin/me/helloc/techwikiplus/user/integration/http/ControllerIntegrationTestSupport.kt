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
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import org.springframework.test.annotation.DirtiesContext

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
    protected fun <T> createJsonHttpEntityWithAuth(body: T?, token: String): HttpEntity<T> {
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
        val path: String
    )
}