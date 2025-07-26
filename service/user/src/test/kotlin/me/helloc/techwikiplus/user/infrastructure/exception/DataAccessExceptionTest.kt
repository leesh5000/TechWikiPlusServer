package me.helloc.techwikiplus.user.infrastructure.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DataAccessException 테스트")
class DataAccessExceptionTest {
    @Test
    @DisplayName("DataAccessException은 작업 설명과 원인을 포함한다")
    fun `data access exception contains operation and cause`() {
        // given
        val operation = "Finding user by email"
        val cause = RuntimeException("Database connection lost")

        // when
        val exception = DataAccessException(operation, cause)

        // then
        assertThat(exception).isInstanceOf(InfrastructureException::class.java)
        assertThat(exception.message).contains(operation)
        assertThat(exception.cause).isEqualTo(cause)
        assertThat(exception.retryable).isFalse() // 데이터베이스 작업은 기본적으로 재시도 불가
    }

    @Test
    @DisplayName("DataAccessException을 재시도 가능으로 설정할 수 있다")
    fun `data access exception can be retryable`() {
        // given
        val operation = "Reading from replica"
        val cause = RuntimeException("Temporary network issue")

        // when
        val exception = DataAccessException(operation, cause, retryable = true)

        // then
        assertThat(exception.retryable).isTrue()
    }
}
