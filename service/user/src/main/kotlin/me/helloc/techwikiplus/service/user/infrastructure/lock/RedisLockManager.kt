package me.helloc.techwikiplus.service.user.infrastructure.lock

import me.helloc.techwikiplus.service.user.domain.service.port.LockManager
import me.helloc.techwikiplus.service.user.domain.service.port.LockManagerException
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class RedisLockManager(
    private val redisTemplate: StringRedisTemplate,
) : LockManager {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisLockManager::class.java)
        private const val LOCK_PREFIX = "distributed_lock:"
        private const val UNLOCK_SCRIPT = """
            if redis.call("GET", KEYS[1]) == ARGV[1] then
                return redis.call("DEL", KEYS[1])
            else
                return 0
            end
        """
    }

    // 스레드 로컬로 각 스레드별 락 식별자 관리
    private val lockValues = ThreadLocal<String>()

    override fun <T> executeWithLock(
        key: String,
        waitTime: Duration,
        leaseTime: Duration,
        block: () -> T,
    ): T {
        val lockKey = LOCK_PREFIX + key
        val lockValue = generateLockValue()

        try {
            val acquired = acquireLock(lockKey, lockValue, waitTime, leaseTime)
            if (!acquired) {
                throw LockManagerException(
                    "Failed to acquire lock for key: $key within ${waitTime.seconds} seconds",
                )
            }

            logger.debug("Lock acquired for key: {}", key)
            lockValues.set(lockValue)

            return block()
        } finally {
            try {
                unlock(lockKey)
                logger.debug("Lock released for key: {}", key)
            } catch (e: Exception) {
                logger.warn("Failed to release lock for key: {}", key, e)
            } finally {
                lockValues.remove()
            }
        }
    }

    override fun tryLock(
        key: String,
        leaseTime: Duration,
    ): Boolean {
        val lockKey = LOCK_PREFIX + key
        val lockValue = generateLockValue()
        lockValues.set(lockValue)

        return try {
            val result =
                redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, leaseTime.toMillis(), TimeUnit.MILLISECONDS)
            result == true
        } catch (e: Exception) {
            logger.error("Error while trying to acquire lock for key: {}", key, e)
            false
        }
    }

    override fun unlock(key: String) {
        val lockKey = if (key.startsWith(LOCK_PREFIX)) key else LOCK_PREFIX + key
        val lockValue = lockValues.get()

        if (lockValue != null) {
            try {
                // Lua 스크립트를 사용하여 원자적으로 락 해제
                val result =
                    redisTemplate.execute<Long> { connection ->
                        connection.eval(
                            UNLOCK_SCRIPT.toByteArray(),
                            org.springframework.data.redis.connection.ReturnType.INTEGER,
                            1,
                            lockKey.toByteArray(),
                            lockValue.toByteArray(),
                        ) as Long?
                    }

                if (result == 1L) {
                    logger.debug("Successfully released lock: {}", lockKey)
                } else {
                    logger.warn("Lock was not owned by current thread: {}", lockKey)
                }
            } catch (e: Exception) {
                logger.error("Error while releasing lock: {}", lockKey, e)
            }
        }
    }

    /**
     * 락 획득을 시도합니다 (재시도 로직 포함)
     */
    private fun acquireLock(
        lockKey: String,
        lockValue: String,
        waitTime: Duration,
        leaseTime: Duration,
    ): Boolean {
        val deadline = System.currentTimeMillis() + waitTime.toMillis()
        val retryInterval = 50L // 50ms 간격으로 재시도

        while (System.currentTimeMillis() < deadline) {
            try {
                val acquired =
                    redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, leaseTime.toMillis(), TimeUnit.MILLISECONDS)

                if (acquired == true) {
                    return true
                }

                // 잠시 대기 후 재시도
                Thread.sleep(retryInterval)
            } catch (e: Exception) {
                logger.error("Error during lock acquisition attempt for key: {}", lockKey, e)
                Thread.sleep(retryInterval)
            }
        }

        return false
    }

    /**
     * 고유한 락 값 생성 (UUID + 스레드ID 조합)
     */
    private fun generateLockValue(): String {
        return "${UUID.randomUUID()}-${Thread.currentThread().id}"
    }
}
