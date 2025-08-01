package me.helloc.techwikiplus.test.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * TestContainers를 Spring Boot 테스트와 통합하기 위한 초기화 클래스
 *
 * MySQL 컨테이너의 동적 설정을 Spring 애플리케이션 컨텍스트에 주입합니다.
 */
class TestContainersInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // MySQL 컨테이너 시작 및 설정 적용
        val properties = MySqlTestContainer.getProperties()

        TestPropertyValues.of(properties)
            .applyTo(applicationContext.environment)
    }
}
