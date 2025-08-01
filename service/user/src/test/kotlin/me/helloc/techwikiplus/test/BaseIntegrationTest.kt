package me.helloc.techwikiplus.test

import me.helloc.techwikiplus.test.config.TestContainersInitializer
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional

/**
 * 통합 테스트를 위한 기본 클래스
 *
 * - TestContainers를 사용한 실제 MySQL 연동
 * - 전체 애플리케이션 컨텍스트 로드
 * - 트랜잭션 롤백으로 테스트 격리
 * - 실제 운영 환경과 유사한 테스트 환경
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@ContextConfiguration(initializers = [TestContainersInitializer::class])
@Transactional
abstract class BaseIntegrationTest {
    /**
     * 테스트 데이터 정리 등 공통 기능 제공 가능
     */
}
