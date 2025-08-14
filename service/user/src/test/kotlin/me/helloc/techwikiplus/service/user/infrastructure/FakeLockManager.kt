package me.helloc.techwikiplus.service.user.infrastructure

import me.helloc.techwikiplus.service.user.domain.service.port.LockManager
import me.helloc.techwikiplus.service.user.domain.service.port.LockManagerException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * 락 없이 동작하는 테스트용 LockManager 구현체
 * 동시성 제어 없이 바로 블록을 실행합니다.
 */
class NoOpLockManager : LockManager {
    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        leaseTime: Duration,
        block: () -> T,
    ): T {
        return block()
    }

    override fun tryLock(
        key: String,
        leaseTime: Duration,
    ): Boolean = true

    override fun unlock(key: String) = Unit
}

/**
 * 순차 실행을 보장하는 테스트용 LockManager 구현체
 * synchronized를 사용하여 실제 분산 락 동작을 시뮬레이션합니다.
 */
class TestLockManager : LockManager {
    private val locks = ConcurrentHashMap<String, Any>()

    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        leaseTime: Duration,
        block: () -> T,
    ): T {
        val lockObject = locks.computeIfAbsent(key) { Any() }

        return synchronized(lockObject) {
            // 실제 락 동작을 시뮬레이션하기 위해 약간의 지연 추가
            Thread.sleep(10)
            block()
        }
    }

    override fun tryLock(
        key: String,
        leaseTime: Duration,
    ): Boolean = true

    override fun unlock(key: String) = Unit
}

/**
 * 항상 타임아웃되는 테스트용 LockManager 구현체
 * 락 획득 실패 시나리오를 테스트할 때 사용합니다.
 */
class SlowLockManager : LockManager {
    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        leaseTime: Duration,
        block: () -> T,
    ): T {
        throw LockManagerException("Lock acquisition timeout for key: $key")
    }

    override fun tryLock(
        key: String,
        leaseTime: Duration,
    ): Boolean = false

    override fun unlock(key: String) = Unit
}

/**
 * 락 획득 횟수를 추적하는 테스트용 LockManager 구현체
 * 테스트에서 락 사용 패턴을 검증할 때 사용합니다.
 */
class TrackingLockManager : LockManager {
    private val lockCounts = ConcurrentHashMap<String, Int>()
    private val locks = ConcurrentHashMap<String, Any>()

    fun getLockCount(key: String): Int = lockCounts[key] ?: 0

    fun resetCounts() {
        lockCounts.clear()
    }

    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        leaseTime: Duration,
        block: () -> T,
    ): T {
        lockCounts[key] = getLockCount(key) + 1
        val lockObject = locks.computeIfAbsent(key) { Any() }

        return synchronized(lockObject) {
            block()
        }
    }

    override fun tryLock(
        key: String,
        leaseTime: Duration,
    ): Boolean {
        lockCounts[key] = getLockCount(key) + 1
        return true
    }

    override fun unlock(key: String) = Unit
}
