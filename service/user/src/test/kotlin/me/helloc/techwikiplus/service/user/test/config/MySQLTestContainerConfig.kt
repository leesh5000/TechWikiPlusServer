package me.helloc.techwikiplus.service.user.test.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * MySQL TestContainer 설정을 위한 ApplicationContextInitializer
 *
 * 이 클래스는 테스트 환경에서 MySQL 컨테이너를 시작하고
 * Spring 애플리케이션 컨텍스트에 데이터베이스 연결 정보를 주입합니다.
 */
class MySQLTestContainerConfig : ApplicationContextInitializer<ConfigurableApplicationContext> {
    companion object {
        private val MYSQL_IMAGE = DockerImageName.parse("mysql:8.0")

        // 싱글톤 패턴으로 컨테이너 재사용
        private val mysqlContainer =
            MySQLContainer(MYSQL_IMAGE).apply {
                withDatabaseName("test_db")
                withUsername("test_user")
                withPassword("test_pass")
                withReuse(true) // 테스트 간 컨테이너 재사용
                start()
            }
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertyValues.of(
            "spring.datasource.url=${mysqlContainer.jdbcUrl}",
            "spring.datasource.username=${mysqlContainer.username}",
            "spring.datasource.password=${mysqlContainer.password}",
            "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect",
            "spring.jpa.show-sql=true",
            "spring.jpa.properties.hibernate.format_sql=true",
        ).applyTo(applicationContext.environment)
    }
}
