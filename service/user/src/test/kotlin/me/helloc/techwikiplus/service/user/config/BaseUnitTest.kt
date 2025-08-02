package me.helloc.techwikiplus.service.user.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * 단위 테스트를 위한 기본 클래스
 *
 * - 외부 의존성을 Fake 객체로 대체
 * - 빠른 실행 속도 보장
 * - 테스트 격리성 보장
 * - FIRST 원칙 준수
 */
@ExtendWith(SpringExtension::class)
@ActiveProfiles("unit-test")
abstract class BaseUnitTest {
    /**
     * 각 테스트 실행 전 Fake 객체 초기화
     * 테스트 간 격리성 보장
     */
    @BeforeEach
    fun resetFakes() {
        // Fake 객체들이 추가되면 여기서 초기화
    }
}
