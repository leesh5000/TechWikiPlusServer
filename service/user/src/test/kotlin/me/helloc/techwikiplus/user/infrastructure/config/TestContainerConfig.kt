package me.helloc.techwikiplus.user.infrastructure.config

import com.redis.testcontainers.RedisContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
open class TestContainerConfig {
    companion object {
        init {
            // CI 환경 감지 및 설정
            val isCI = System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null
            if (isCI) {
                System.setProperty("testcontainers.reuse.enable", "false")
                System.setProperty("testcontainers.startup.timeout", "300")
            }
        }
        // MySQL 컨테이너 - 여러 테스트에서 재사용하여 성능 향상
        @JvmStatic
        val mysqlContainer: MySQLContainer<*> =
            MySQLContainer("mysql:8.0")
                .withDatabaseName("techwikiplus")
                .withUsername("techwikiplus")
                .withPassword("techwikiplus")
                .withReuse(true)
                .withStartupTimeoutSeconds(300) // CI 환경을 위한 타임아웃 증가
                .withCommand("--default-authentication-plugin=mysql_native_password")
                .apply { start() }

        // Redis 컨테이너 - 여러 테스트에서 재사용하여 성능 향상
        @JvmStatic
        val redisContainer: RedisContainer =
            RedisContainer(
                DockerImageName.parse("redis:7-alpine"),
            )
                .withReuse(true)
                .withStartupTimeoutSeconds(300) // CI 환경을 위한 타임아웃 증가
                .apply { start() }

        // Spring의 동적 프로퍼티 설정 - 컨테이너의 실제 포트를 애플리케이션에 주입
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            // MySQL 연결 정보 설정
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }

            // JPA 설정 - 테스트 시 테이블 자동 생성
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            // Hibernate가 자동으로 dialect를 감지하므로 명시적 지정 불필요

            // Redis 연결 정보 설정
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
        }
    }
}
