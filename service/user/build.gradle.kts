group = "me.helloc.techwikiplus"
version = "0.0.1"

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

    // Spring Boot Actuator - 헬스체크 및 모니터링
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring Boot Configuration Processor - IDE에서 설정 프로퍼티 자동 완성 지원
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // mysql
    implementation("com.mysql:mysql-connector-j")

    // H2 Database - 문서화 테스트용
    testImplementation("com.h2database:h2")

    // Embedded Redis - 문서화 테스트용
    testImplementation("it.ozimov:embedded-redis:0.7.3") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }

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

    // Spring REST Docs - 테스트 주도 API 문서화
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")

    // restdocs-api-spec - Spring REST Docs를 OpenAPI 스펙으로 변환
    testImplementation("com.epages:restdocs-api-spec:0.19.4")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.4")

    // Swagger UI - API 문서 UI (런타임에 제공)
    implementation("org.webjars:swagger-ui:5.10.3")
}

springBoot {
    buildInfo()
}

// REST Docs 출력 디렉토리 설정
val snippetsDir = file("build/generated-snippets")

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

    // REST Docs 출력 디렉토리 설정
    outputs.dir(snippetsDir)
}

// OpenAPI 3.0 스펙 생성 태스크
tasks.register<JavaExec>("openapi3") {
    dependsOn(tasks.test, tasks.classes, tasks.compileKotlin)

    mainClass.set("me.helloc.techwikiplus.user.infrastructure.documentation.OpenApiGenerator")
    classpath = sourceSets["main"].runtimeClasspath

    val snippetsDir = file("$buildDir/generated-snippets")
    val openApiFile = file("$buildDir/api-spec/openapi3.json")

    args =
        listOf(
            snippetsDir.absolutePath,
            openApiFile.absolutePath,
            "TechWikiPlus User Service API",
            "사용자 인증 및 관리 서비스 API",
            "v1",
            "http://localhost:9000",
        )

    doFirst {
        println("=== OpenAPI Generator Task ===")
        println("Snippets directory: ${snippetsDir.absolutePath}")
        println("Snippets directory exists: ${snippetsDir.exists()}")

        if (snippetsDir.exists()) {
            val snippetFiles = snippetsDir.walk().filter { it.name == "resource.json" }.toList()
            println("Number of resource.json files found: ${snippetFiles.size}")
            snippetFiles.forEach { file ->
                println("  - ${file.absolutePath}")
            }
        }

        if (!snippetsDir.exists() || snippetsDir.listFiles()?.isEmpty() == true) {
            throw GradleException("REST Docs 스니펫이 없습니다. 테스트를 먼저 실행해주세요.")
        }

        println("Output file: ${openApiFile.absolutePath}")
        println("==============================")
    }

    // 표준 출력과 에러를 콘솔에 표시
    standardOutput = System.out
    errorOutput = System.err
}

// OpenAPI 스펙을 정적 리소스로 복사 - Configuration Cache 호환
tasks.register<Sync>("copyOpenApiSpec") {
    dependsOn("openapi3")

    from("$buildDir/api-spec") {
        include("*.json")
    }

    into("src/main/resources/static/docs")

    // Sync 태스크는 대상 디렉토리를 자동으로 생성합니다
}
