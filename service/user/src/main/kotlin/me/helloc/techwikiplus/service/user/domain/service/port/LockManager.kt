package me.helloc.techwikiplus.service.user.domain.service.port

import java.time.Duration

/**
 * 락 관리 인터페이스
 */
interface LockManager {
    /**
     * 지정된 키로 락을 획득하고 블록을 실행합니다.
     *
     * @param key 락 키
     * @param waitTime 락 획득 대기 시간
     * @param leaseTime 락 유지 시간 (자동 해제)
     * @param block 락을 획득한 후 실행할 블록
     * @return 블록의 실행 결과
     * @throws LockManagerException 락 획득 실패 시
     */
    fun <T> executeWithLock(
        key: String,
        waitTime: Duration = Duration.ofSeconds(3),
        leaseTime: Duration = Duration.ofSeconds(10),
        block: () -> T,
    ): T

    /**
     * 락 획득을 시도합니다.
     *
     * @param key 락 키
     * @param leaseTime 락 유지 시간
     * @return 락 획득 성공 여부
     */
    fun tryLock(
        key: String,
        leaseTime: Duration = Duration.ofSeconds(10),
    ): Boolean

    /**
     * 락을 해제합니다.
     *
     * @param key 락 키
     */
    fun unlock(key: String)
}

/**
 * 락 관리 관련 예외
 */
class LockManagerException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
