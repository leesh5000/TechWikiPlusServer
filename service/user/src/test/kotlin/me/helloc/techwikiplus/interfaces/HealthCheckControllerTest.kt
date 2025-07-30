package me.helloc.techwikiplus.interfaces

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-user",
        "spring.application.version=1.0.0-TEST",
    ],
)
class HealthCheckControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `should return health status with version information`() {
        webTestClient.get()
            .uri("/health")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.version").isEqualTo("1.0.0-TEST")
            .jsonPath("$.serviceName").isEqualTo("techwikiplus-user")
    }
}
