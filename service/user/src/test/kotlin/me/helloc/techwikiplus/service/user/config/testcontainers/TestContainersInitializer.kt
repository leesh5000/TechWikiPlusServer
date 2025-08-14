package me.helloc.techwikiplus.service.user.config.testcontainers

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * TestContainers를 Spring Boot 테스트와 통합하기 위한 초기화 클래스
 *
 * MySQL, Redis 및 MailHog 컨테이너의 동적 설정을 Spring 애플리케이션 컨텍스트에 주입합니다.
 */
class TestContainersInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // MySQL 컨테이너 시작 및 설정 적용
        val mysqlProperties = MySqlTestContainer.getProperties()

        // Redis 컨테이너 시작 및 설정 적용
        val redisProperties = RedisTestContainer.getProperties()

        // MailHog 컨테이너 시작 및 설정 적용
        val mailhogProperties = MailHogTestContainer.getProperties()

        // 모든 TestContainer 설정 병합
        val properties = mysqlProperties + redisProperties + mailhogProperties

        TestPropertyValues.of(properties)
            .applyTo(applicationContext.environment)
    }
}
