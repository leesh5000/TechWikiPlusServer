package me.helloc.techwikiplus.user.domain.port.outbound

/**
 * ID 생성을 위한 도메인 서비스 인터페이스
 *
 * 도메인 계층에서 구체적인 ID 생성 구현체에 의존하지 않도록 추상화
 */
interface IdGenerator {
    /**
     * 고유한 ID를 생성한다.
     *
     * @return 생성된 고유 ID
     */
    fun next(): Long
}
