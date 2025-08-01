group = "${rootProject.group}.user"

plugins {
    // RestDocs API Spec - Spring REST Docs와 OpenAPI 통합
    id("com.epages.restdocs-api-spec") version "0.19.0"
}

repositories {
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {

    // Spring Boot Web - REST API 개발
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Boot Data JPA - 데이터베이스 연동
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Common modules
    implementation(project(":common:snowflake"))

    // DB
    runtimeOnly("com.mysql:mysql-connector-j")

    // Logging & Tracing
    // Structured logging with JSON format for log aggregation
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    // Coroutine MDC support for correlation ID propagation
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.6.0")
    // Kotlin logging facade
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Spring Boot 테스트 지원
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // WebTestClient를 위한 WebFlux 의존성
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    // Mockito-Kotlin for better Kotlin support
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    // TestContainers 핵심 라이브러리
    testImplementation("org.testcontainers:testcontainers")
    // JUnit5 통합을 위한 TestContainers 확장
    testImplementation("org.testcontainers:junit-jupiter")
    // MySQL 컨테이너 - JPA Repository 통합 테스트용
    testImplementation("org.testcontainers:mysql")
    // Kotlin 데이터 클래스 직렬화 지원
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring REST Docs
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")

    // RestDocs API Spec - Spring REST Docs와 OpenAPI 통합
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.0")

    // Swagger UI - OpenAPI 문서 시각화
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
}

// OpenAPI 3.0.1 문서 생성 설정
openapi3 {
    val protocol = System.getenv("PROTOCOL") ?: "http"
    val host = System.getenv("SERVER_HOST") ?: "localhost"
    val port = System.getenv("SERVER_PORT") ?: "9000"

    // 단일 서버 설정
    setServer("$protocol://$host:$port")

    title = "TechWikiPlus User Service API"
    description = "User Service API Documentation"
    version = System.getenv("IMAGE_TAG") ?: "LOCAL_VERSION"
    format = "yml"
    outputDirectory = "build/api-spec"
    snippetsDirectory = "build/generated-snippets"
}

// OpenAPI 문서를 정적 리소스로 복사하는 태스크
tasks.register<Copy>("copyOpenApiToResources") {
    dependsOn("openapi3")
    from("build/api-spec/openapi3.yml")
    into("src/main/resources/static/api-docs")
}

// 테스트 실행 후 자동으로 OpenAPI 문서 생성 및 복사
tasks.named("test") {
    finalizedBy("copyOpenApiToResources")
}
