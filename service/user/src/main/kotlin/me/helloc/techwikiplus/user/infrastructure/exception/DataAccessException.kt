package me.helloc.techwikiplus.user.infrastructure.exception

/**
 * 데이터베이스 접근 중 발생하는 예외
 *
 * @property operation 수행하려던 작업 설명
 * @property cause 원인이 되는 예외
 * @property retryable 재시도 가능 여부 (기본값: false)
 */
class DataAccessException(
    operation: String,
    cause: Throwable,
    retryable: Boolean = false,
) : InfrastructureException(
        message = "Data access error during: $operation",
        cause = cause,
        retryable = retryable,
    )
