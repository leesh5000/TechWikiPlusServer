group = "${rootProject.group}.user"

dependencies {

    // Spring Boot Web - REST API 개발
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Boot Data JPA - 데이터베이스 연동
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Common modules
    implementation(project(":common:snowflake"))

    // DB
    runtimeOnly("com.mysql:mysql-connector-j")

    // Spring Boot 테스트 지원
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // WebTestClient를 위한 WebFlux 의존성
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    // TestContainers 핵심 라이브러리
    testImplementation("org.testcontainers:testcontainers")
    // JUnit5 통합을 위한 TestContainers 확장
    testImplementation("org.testcontainers:junit-jupiter")
    // MySQL 컨테이너 - JPA Repository 통합 테스트용
    testImplementation("org.testcontainers:mysql")
    // Kotlin 데이터 클래스 직렬화 지원
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
