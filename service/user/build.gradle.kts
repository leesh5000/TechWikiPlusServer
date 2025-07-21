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
}
