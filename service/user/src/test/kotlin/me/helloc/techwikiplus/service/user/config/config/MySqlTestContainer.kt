package me.helloc.techwikiplus.service.user.config.config

import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * MySQL TestContainer 싱글톤 관리
 *
 * 테스트 실행 시 하나의 MySQL 컨테이너를 재사용하여 성능을 향상시킵니다.
 * 컨테이너는 JVM이 종료될 때까지 유지되며, 모든 통합 테스트에서 공유됩니다.
 */
object MySqlTestContainer {
    private const val MYSQL_VERSION = "mysql:8.0"
    private const val DATABASE_NAME = "testdb"
    private const val USERNAME = "test"
    private const val PASSWORD = "test"

    val instance: MySQLContainer<*> by lazy {
        MySQLContainer(DockerImageName.parse(MYSQL_VERSION))
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withReuse(true)
            .withLabel("testcontainers.reuse.enable", "true")
            .apply {
                start()
            }
    }

    /**
     * Spring Boot가 사용할 데이터소스 설정을 동적으로 제공
     * 모든 테스트에서 동일한 설정을 사용하여 일관성 유지
     */
    fun getProperties(): Map<String, String> =
        mapOf(
            "spring.datasource.url" to instance.jdbcUrl,
            "spring.datasource.username" to instance.username,
            "spring.datasource.password" to instance.password,
            "spring.datasource.driver-class-name" to "com.mysql.cj.jdbc.Driver",
            "spring.jpa.hibernate.ddl-auto" to "create-drop",
            "spring.jpa.properties.hibernate.dialect" to "org.hibernate.dialect.MySQLDialect",
            "spring.jpa.show-sql" to "true",
            "spring.jpa.properties.hibernate.format_sql" to "true",
        )
}
