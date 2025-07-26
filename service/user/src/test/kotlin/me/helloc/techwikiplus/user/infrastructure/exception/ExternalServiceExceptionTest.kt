package me.helloc.techwikiplus.user.infrastructure.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ExternalServiceException 테스트")
class ExternalServiceExceptionTest {
    @Test
    @DisplayName("ExternalServiceException은 서비스 이름과 원인을 포함한다")
    fun `external service exception contains service name and cause`() {
        // given
        val serviceName = "Authentication Service"
        val cause = RuntimeException("Connection timeout")

        // when
        val exception = ExternalServiceException(serviceName, cause)

        // then
        assertThat(exception).isInstanceOf(InfrastructureException::class.java)
        assertThat(exception.message).contains(serviceName)
        assertThat(exception.cause).isEqualTo(cause)
        assertThat(exception.retryable).isTrue() // 외부 서비스는 보통 재시도 가능
    }

    @Test
    @DisplayName("ExternalServiceException은 재시도 불가능으로 설정할 수 있다")
    fun `external service exception can be non-retryable`() {
        // given
        val serviceName = "Payment Service"
        val cause = RuntimeException("Invalid API key")

        // when
        val exception = ExternalServiceException(serviceName, cause, retryable = false)

        // then
        assertThat(exception.retryable).isFalse()
    }
}
