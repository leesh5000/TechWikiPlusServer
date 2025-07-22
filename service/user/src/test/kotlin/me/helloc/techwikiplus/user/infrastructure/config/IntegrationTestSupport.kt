package me.helloc.techwikiplus.user.infrastructure.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 통합 테스트를 위한 기본 클래스
 *
 * TestContainers를 사용하여 실제 MySQL과 Redis 환경에서 테스트 수행
 * 상속받는 테스트 클래스는 자동으로 컨테이너 환경 설정을 적용받음
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestContainerConfig::class)
abstract class IntegrationTestSupport {
    init {
        // 컨테이너가 시작되었는지 확인
        require(TestContainerConfig.mysqlContainer.isRunning) {
            "MySQL container must be running"
        }
        require(TestContainerConfig.redisContainer.isRunning) {
            "Redis container must be running"
        }
    }
}
