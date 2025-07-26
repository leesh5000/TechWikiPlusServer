package me.helloc.techwikiplus.user.infrastructure.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("InfrastructureException 테스트")
class InfrastructureExceptionTest {
    @Test
    @DisplayName("InfrastructureException은 기본적으로 재시도 불가능하다")
    fun `infrastructure exception is not retryable by default`() {
        // given
        val message = "Infrastructure error occurred"
        val cause = RuntimeException("Original error")

        // when
        val exception = InfrastructureException(message, cause)

        // then
        assertThat(exception).isInstanceOf(RuntimeException::class.java)
        assertThat(exception.message).isEqualTo(message)
        assertThat(exception.cause).isEqualTo(cause)
        assertThat(exception.retryable).isFalse()
    }

    @Test
    @DisplayName("InfrastructureException은 재시도 가능 여부를 설정할 수 있다")
    fun `infrastructure exception can be set as retryable`() {
        // given
        val message = "Temporary network error"
        val cause = RuntimeException("Connection timeout")

        // when
        val exception = InfrastructureException(message, cause, retryable = true)

        // then
        assertThat(exception.retryable).isTrue()
    }

    @Test
    @DisplayName("InfrastructureException은 원인 예외 없이 생성할 수 있다")
    fun `infrastructure exception can be created without cause`() {
        // given
        val message = "Configuration error"

        // when
        val exception = InfrastructureException(message)

        // then
        assertThat(exception.message).isEqualTo(message)
        assertThat(exception.cause).isNull()
        assertThat(exception.retryable).isFalse()
    }
}
