package me.helloc.techwikiplus.service.user.adapter.outbound.test.config

import me.helloc.techwikiplus.service.user.adapter.outbound.test.config.config.MySqlTestContainer
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * MySQL TestContainer 설정을 위한 ApplicationContextInitializer
 *
 * 이 클래스는 통합된 MySqlTestContainer를 사용하여
 * Spring 애플리케이션 컨텍스트에 데이터베이스 연결 정보를 주입합니다.
 */
class MySQLTestContainerConfig : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // 통합된 MySqlTestContainer 사용
        val properties = MySqlTestContainer.getProperties()
        TestPropertyValues.of(properties).applyTo(applicationContext.environment)
    }
}
