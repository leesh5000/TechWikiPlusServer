dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Snowflake ID generator
    implementation(project(":common:snowflake"))

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    // redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // mail
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // thymeleaf for email templates
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // mysql
    implementation("com.mysql:mysql-connector-j")

    // TestContainers - 통합 테스트를 위한 도커 기반 테스트 환경
    // BOM(Bill of Materials): TestContainers 모듈들의 버전을 일관되게 관리
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.3"))

    // TestContainers 핵심 라이브러리
    testImplementation("org.testcontainers:testcontainers")

    // JUnit5 통합을 위한 TestContainers 확장
    testImplementation("org.testcontainers:junit-jupiter")

    // MySQL 컨테이너 - JPA Repository 통합 테스트용
    testImplementation("org.testcontainers:mysql")

    // Redis 컨테이너 - VerificationCodeStore 통합 테스트용
    testImplementation("com.redis.testcontainers:testcontainers-redis:1.6.4")

    // ArchUnit - 아키텍처 검증 도구
    testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
}

tasks.test {
    // Disable IntelliJ capture agent for WSL compatibility
    jvmArgs =
        listOf(
            "-Djava.security.manager=allow",
            "-Didea.no.capture.agent=true",
        )

    // Disable agent loading
    systemProperty("idea.test.cyclic.buffer.size", "disabled")

    // Remove any capture agent from classpath
    environment("JAVA_TOOL_OPTIONS", "")
}
