package me.helloc.techwikiplus.service.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * MySQL TestContainer를 사용하는 JPA 통합 테스트를 위한 추상 베이스 클래스
 *
 * 이 클래스를 상속받으면 자동으로 MySQL TestContainer가 설정되며,
 * Spring Data JPA 테스트 환경이 구성됩니다.
 *
 * @see MySQLTestContainerConfig
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = [MySQLTestContainerConfig::class])
@Testcontainers
abstract class MySQLTestContainerBase : FunSpec() {
    init {
        extensions(SpringExtension)
    }
}
