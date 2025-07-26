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

// 정적 리소스 디렉토리 생성
tasks.register("createDocsDir") {
    doLast {
        file("src/main/resources/static/docs").mkdirs()
    }
}

// OpenAPI 3.0 스펙 생성 태스크
tasks.register("openapi3") {
    dependsOn(tasks.test)
    doLast {
        val openApiDir = file("$buildDir/api-spec")
        openApiDir.mkdirs()

        // restdocs-api-spec snippet files 읽기
        val snippetsDir = file("$buildDir/generated-snippets")

        if (snippetsDir.exists() && snippetsDir.listFiles()?.isNotEmpty() == true) {
            println("REST Docs 스니펫을 찾았습니다. OpenAPI 스펙을 생성합니다...")

            // 간단하게 Jackson으로 resource.json 파일들을 읽어서 OpenAPI 스펙 생성
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            val openApiSpec = mapper.createObjectNode()

            // OpenAPI 기본 정보
            openApiSpec.put("openapi", "3.0.1")

            val infoNode = mapper.createObjectNode()
            infoNode.put("title", "TechWikiPlus User Service API")
            infoNode.put("description", "사용자 인증 및 관리 서비스 API")
            infoNode.put("version", "v1")
            openApiSpec.set<com.fasterxml.jackson.databind.node.ObjectNode>("info", infoNode)

            // 서버 정보
            val serversArray = mapper.createArrayNode()
            val serverNode = mapper.createObjectNode()
            serverNode.put("url", "http://localhost:9000")
            serverNode.put("description", "Local development server")
            serversArray.add(serverNode)
            openApiSpec.set<com.fasterxml.jackson.databind.node.ArrayNode>("servers", serversArray)

            // paths 노드
            val pathsNode = mapper.createObjectNode()

            // components 노드
            val componentsNode = mapper.createObjectNode()
            val schemasNode = mapper.createObjectNode()

            // 에러 응답 스키마 추가
            val errorResponseSchema = mapper.createObjectNode()
            errorResponseSchema.put("type", "object")
            val errorPropertiesNode = mapper.createObjectNode()

            errorPropertiesNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                "errorCode",
                mapper.createObjectNode().apply {
                    put("type", "string")
                    put("description", "에러 코드")
                },
            )
            errorPropertiesNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                "message",
                mapper.createObjectNode().apply {
                    put("type", "string")
                    put("description", "에러 메시지")
                },
            )
            errorPropertiesNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                "timestamp",
                mapper.createObjectNode().apply {
                    put("type", "string")
                    put("description", "에러 발생 시간")
                },
            )
            errorPropertiesNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                "path",
                mapper.createObjectNode().apply {
                    put("type", "string")
                    put("description", "요청 경로")
                },
            )
            errorPropertiesNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                "localizedMessage",
                mapper.createObjectNode().apply {
                    put("type", "string")
                    put("description", "현지화된 메시지")
                },
            )
            errorPropertiesNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                "details",
                mapper.createObjectNode().apply {
                    put("type", "object")
                    put("description", "추가 에러 정보")
                },
            )

            errorResponseSchema.set<com.fasterxml.jackson.databind.node.ObjectNode>("properties", errorPropertiesNode)
            schemasNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("ErrorResponse", errorResponseSchema)

            // 모든 resource.json 파일 처리
            snippetsDir.walk()
                .filter { it.name == "resource.json" }
                .forEach { resourceFile ->
                    try {
                        val resource = mapper.readTree(resourceFile)
                        val path = resource.path("request").path("path").asText()
                        val method = resource.path("request").path("method").asText().lowercase()

                        // path가 없으면 생성
                        val pathNode =
                            pathsNode.path(path) as? com.fasterxml.jackson.databind.node.ObjectNode
                                ?: mapper.createObjectNode()

                        // method 노드 생성
                        val methodNode = mapper.createObjectNode()
                        methodNode.put("summary", resource.path("summary").asText())
                        methodNode.put("description", resource.path("description").asText())
                        methodNode.put("operationId", resource.path("operationId").asText())

                        // tags
                        val tagsArray = mapper.createArrayNode()
                        tagsArray.add("User")
                        methodNode.set<com.fasterxml.jackson.databind.node.ArrayNode>("tags", tagsArray)

                        // request body
                        val requestFields = resource.path("request").path("requestFields")
                        if (requestFields.size() > 0) {
                            val requestBodyNode = mapper.createObjectNode()
                            requestBodyNode.put("required", true)
                            val contentNode = mapper.createObjectNode()
                            val jsonContentNode = mapper.createObjectNode()
                            val schemaNode = mapper.createObjectNode()
                            schemaNode.put("type", "object")

                            val propertiesNode = mapper.createObjectNode()
                            val requiredArray = mapper.createArrayNode()

                            requestFields.forEach { field ->
                                val fieldName = field.path("path").asText()
                                val fieldNode = mapper.createObjectNode()
                                fieldNode.put("type", field.path("type").asText().lowercase())
                                fieldNode.put("description", field.path("description").asText())
                                propertiesNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(fieldName, fieldNode)
                                if (!field.path("optional").asBoolean()) {
                                    requiredArray.add(fieldName)
                                }
                            }

                            schemaNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("properties", propertiesNode)
                            schemaNode.set<com.fasterxml.jackson.databind.node.ArrayNode>("required", requiredArray)
                            jsonContentNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("schema", schemaNode)
                            contentNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                                "application/json",
                                jsonContentNode,
                            )
                            requestBodyNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("content", contentNode)
                            methodNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                                "requestBody",
                                requestBodyNode,
                            )
                        }

                        // responses
                        val responsesNode = mapper.createObjectNode()
                        val statusCode = resource.path("response").path("status").asText()
                        val responseNode = mapper.createObjectNode()
                        responseNode.put(
                            "description",
                            if (statusCode == "202") {
                                "요청 성공"
                            } else if (statusCode.startsWith("4")) {
                                "요청 실패"
                            } else {
                                "서버 오류"
                            },
                        )

                        // response headers
                        val responseHeaders = resource.path("response").path("headers")
                        if (responseHeaders.size() > 0) {
                            val headersNode = mapper.createObjectNode()
                            responseHeaders.forEach { header ->
                                val headerNode = mapper.createObjectNode()
                                headerNode.put("description", header.path("description").asText())
                                val schemaNode = mapper.createObjectNode()
                                schemaNode.put("type", header.path("type").asText().lowercase())
                                headerNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("schema", schemaNode)
                                headersNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                                    header.path("name").asText(),
                                    headerNode,
                                )
                            }
                            responseNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("headers", headersNode)
                        }

                        // response body for error cases
                        if (statusCode.startsWith("4") || statusCode.startsWith("5")) {
                            val contentNode = mapper.createObjectNode()
                            val jsonContentNode = mapper.createObjectNode()
                            val schemaNode = mapper.createObjectNode()
                            schemaNode.put("\$ref", "#/components/schemas/ErrorResponse")
                            jsonContentNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("schema", schemaNode)
                            contentNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(
                                "application/json",
                                jsonContentNode,
                            )
                            responseNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("content", contentNode)
                        }

                        responsesNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(statusCode, responseNode)
                        methodNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("responses", responsesNode)

                        pathNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(method, methodNode)
                        pathsNode.set<com.fasterxml.jackson.databind.node.ObjectNode>(path, pathNode)

                        println("처리된 API: $method $path")
                    } catch (e: Exception) {
                        println("Error processing resource file ${resourceFile.path}: ${e.message}")
                    }
                }

            openApiSpec.set<com.fasterxml.jackson.databind.node.ObjectNode>("paths", pathsNode)
            componentsNode.set<com.fasterxml.jackson.databind.node.ObjectNode>("schemas", schemasNode)
            openApiSpec.set<com.fasterxml.jackson.databind.node.ObjectNode>("components", componentsNode)

            val openApiFile = file("$openApiDir/openapi3.json")
            openApiFile.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(openApiSpec))
            println("OpenAPI 스펙이 생성되었습니다: $openApiFile")
        } else {
            println("REST Docs 스니펫이 없습니다. 테스트를 먼저 실행해주세요.")
        }
    }
}

// OpenAPI 스펙을 정적 리소스로 복사
tasks.register<Copy>("copyOpenApiSpec") {
    dependsOn("openapi3", "createDocsDir")
    from("$buildDir/api-spec")
    into("src/main/resources/static/docs")
    include("*.json")
}
