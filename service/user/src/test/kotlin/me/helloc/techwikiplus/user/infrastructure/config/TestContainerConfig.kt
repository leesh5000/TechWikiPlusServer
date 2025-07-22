package me.helloc.techwikiplus.user.infrastructure.config

import com.redis.testcontainers.RedisContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
class TestContainerConfig {
    companion object {
        // MySQL 컨테이너 - 여러 테스트에서 재사용하여 성능 향상
        @JvmStatic
        val mysqlContainer: MySQLContainer<*> =
            MySQLContainer("mysql:8.0")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)
                .apply { start() }

        // Redis 컨테이너 - 여러 테스트에서 재사용하여 성능 향상
        @JvmStatic
        val redisContainer: RedisContainer =
            RedisContainer(
                DockerImageName.parse("redis:7-alpine"),
            )
                .withReuse(true)
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
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.MySQL8Dialect" }

            // Redis 연결 정보 설정
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
        }
    }
}
