package me.helloc.techwikiplus.domain.correlation

import java.util.UUID

/**
 * Correlation ID 값 객체
 *
 * 분산 추적을 위한 고유 식별자를 나타내는 불변 값 객체입니다.
 * 모든 로그와 서비스 간 통신에서 요청을 추적하는 데 사용됩니다..
 *
 * @property value UUID 형식의 correlation ID 값
 * @throws IllegalArgumentException 빈 문자열이거나 유효하지 않은 UUID 형식인 경우
 */
data class CorrelationId(val value: String) {
    init {
        require(value.isNotBlank()) { "Correlation ID cannot be blank" }
        try {
            UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid UUID format: $value")
        }
    }

    override fun toString(): String = value

    companion object {
        /**
         * 새로운 UUID 기반 CorrelationId를 생성합니다.
         *
         * @return 랜덤하게 생성된 CorrelationId
         */
        fun generate(): CorrelationId = CorrelationId(UUID.randomUUID().toString())
    }
}
