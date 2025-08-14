package me.helloc.techwikiplus.service.apigateway

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayApplicationTest : FunSpec({

    test("Context loads successfully") {
        // 컨텍스트가 성공적으로 로드되는지 확인하는 기본 테스트
        true shouldBe true
    }
})
