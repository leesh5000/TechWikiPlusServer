package me.helloc.techwikiplus.user.infrastructure.exception

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * 인프라 예외 관련 테스트를 통합적으로 실행하는 테스트 스위트
 * 이를 통해 인프라 예외 처리가 제대로 구현되었는지 확인
 */
@DisplayName("인프라 예외 처리 통합 테스트")
class InfrastructureExceptionTestSuite {
    @Nested
    @DisplayName("기본 인프라 예외 테스트")
    inner class BasicExceptionTest {
        @Test
        @DisplayName("InfrastructureException 클래스가 올바르게 생성되는지 확인")
        fun `infrastructure exception classes are created correctly`() {
            assertDoesNotThrow {
                // 기본 InfrastructureException
                InfrastructureException("Test error")
                InfrastructureException("Test error", RuntimeException())
                InfrastructureException("Test error", RuntimeException(), true)

                // MailDeliveryException
                MailDeliveryException("user@example.com")
                MailDeliveryException("user@example.com", RuntimeException())

                // ExternalServiceException
                ExternalServiceException("Redis", RuntimeException())
                ExternalServiceException("Redis", RuntimeException(), false)

                // DataAccessException
                DataAccessException("Finding user", RuntimeException())
                DataAccessException("Finding user", RuntimeException(), true)
            }
        }
    }

    @Test
    @DisplayName("인프라 예외 체계가 제대로 구축되었는지 확인")
    fun `infrastructure exception hierarchy is properly established`() {
        // 각 예외가 InfrastructureException을 상속하는지 확인
        assert(MailDeliveryException("test@example.com") is InfrastructureException)
        assert(ExternalServiceException("Service", RuntimeException()) is InfrastructureException)
        assert(DataAccessException("Operation", RuntimeException()) is InfrastructureException)

        // 모든 예외가 RuntimeException인지 확인
        assert(InfrastructureException("Test") is RuntimeException)
    }
}
