package me.helloc.techwikiplus.user.infrastructure.exception

/**
 * 인프라 계층에서 발생하는 기술적 예외를 나타내는 기본 클래스
 *
 * @property message 예외 메시지
 * @property cause 원인이 되는 예외 (선택적)
 * @property retryable 재시도 가능 여부
 */
open class InfrastructureException(
    message: String,
    cause: Throwable? = null,
    val retryable: Boolean = false,
) : RuntimeException(message, cause)
