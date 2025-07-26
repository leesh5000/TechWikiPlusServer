package me.helloc.techwikiplus.user.infrastructure.exception

/**
 * 메일 발송 중 발생하는 예외
 *
 * @property recipient 메일 수신자 주소
 * @property cause 원인이 되는 예외 (선택적)
 */
class MailDeliveryException(
    recipient: String,
    cause: Throwable? = null,
) : InfrastructureException(
        message = "Failed to deliver mail to: $recipient",
        cause = cause,
        retryable = true,
    )
