extra["springCloudVersion"] = "2025.0.0"

dependencies {
    // Spring Cloud Gateway (API Gateway 핵심 의존성) - 새로운 권장 의존성
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    // Spring Boot Actuator (헬스체크, 메트릭)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Reactive Redis (레이트 리미팅 및 캐싱용)
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Circuit Breaker (장애 복구)
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    // Monitoring (프로메테우스 메트릭)
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Reactive Programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Test Dependencies (추가적인 테스트 의존성)
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")

    // MockK for mocking
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    // WireMock for integration tests
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.1")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}
