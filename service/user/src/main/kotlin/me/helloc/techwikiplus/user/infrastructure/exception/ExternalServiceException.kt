package me.helloc.techwikiplus.user.infrastructure.exception

/**
 * 외부 서비스 호출 중 발생하는 예외
 *
 * @property serviceName 외부 서비스 이름
 * @property cause 원인이 되는 예외
 * @property retryable 재시도 가능 여부 (기본값: true)
 */
class ExternalServiceException(
    serviceName: String,
    cause: Throwable,
    retryable: Boolean = true,
) : InfrastructureException(
        message = "External service error: $serviceName",
        cause = cause,
        retryable = retryable,
    )
