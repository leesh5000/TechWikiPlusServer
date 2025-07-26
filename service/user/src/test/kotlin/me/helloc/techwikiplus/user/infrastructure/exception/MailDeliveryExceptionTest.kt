package me.helloc.techwikiplus.user.infrastructure.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MailDeliveryException 테스트")
class MailDeliveryExceptionTest {
    @Test
    @DisplayName("MailDeliveryException은 메일 주소와 원인을 포함한다")
    fun `mail delivery exception contains recipient and cause`() {
        // given
        val recipient = "user@example.com"
        val cause = RuntimeException("SMTP connection failed")

        // when
        val exception = MailDeliveryException(recipient, cause)

        // then
        assertThat(exception).isInstanceOf(InfrastructureException::class.java)
        assertThat(exception.message).contains(recipient)
        assertThat(exception.cause).isEqualTo(cause)
        assertThat(exception.retryable).isTrue() // 메일 발송은 보통 재시도 가능
    }

    @Test
    @DisplayName("MailDeliveryException은 원인 없이 생성할 수 있다")
    fun `mail delivery exception can be created without cause`() {
        // given
        val recipient = "user@example.com"

        // when
        val exception = MailDeliveryException(recipient)

        // then
        assertThat(exception.message).contains(recipient)
        assertThat(exception.cause).isNull()
        assertThat(exception.retryable).isTrue()
    }
}
