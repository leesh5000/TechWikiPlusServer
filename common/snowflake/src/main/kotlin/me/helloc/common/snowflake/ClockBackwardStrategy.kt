package me.helloc.common.snowflake

/**
 * 시계 역행 상황을 처리하는 전략 인터페이스
 */
interface ClockBackwardStrategy {
    /**
     * 시계 역행 상황을 처리하고 사용할 시간을 반환한다.
     * 
     * @param lastTimeMillis 마지막으로 사용된 시간
     * @param currentTimeMillis 현재 시스템 시간 (역행된 시간)
     * @param timeProvider 시간 제공자
     * @return 사용할 시간 (밀리초)
     * @throws ClockBackwardException 복구할 수 없는 경우
     */
    fun handleClockBackward(
        lastTimeMillis: Long,
        currentTimeMillis: Long,
        timeProvider: TimeProvider,
    ): Long
}

/**
 * 시계가 정상으로 돌아올 때까지 대기하는 전략
 */
class WaitStrategy(
    private val maxWaitTimeMillis: Long = 5000L,
) : ClockBackwardStrategy {

    override fun handleClockBackward(
        lastTimeMillis: Long,
        currentTimeMillis: Long,
        timeProvider: TimeProvider,
    ): Long {
        val startWaitTime = System.currentTimeMillis()
        var currentTime = currentTimeMillis

        while (currentTime <= lastTimeMillis) {
            val waitedTime = System.currentTimeMillis() - startWaitTime
            if (waitedTime > maxWaitTimeMillis) {
                throw ClockBackwardException(
                    lastTimeMillis = lastTimeMillis,
                    currentTimeMillis = currentTime,
                    message = "Clock backward wait timeout after ${waitedTime}ms"
                )
            }

            // 짧은 시간 대기 후 재시도
            Thread.sleep(1L)
            currentTime = timeProvider.currentTimeMillis()
        }

        return currentTime
    }
}

/**
 * 시퀀스를 증가시켜 고유성을 보장하는 전략
 * 주의: 이 전략은 동일한 시간에 대해 더 많은 시퀀스를 소비하므로 신중히 사용해야 함
 */
class SequenceStrategy : ClockBackwardStrategy {

    override fun handleClockBackward(
        lastTimeMillis: Long,
        currentTimeMillis: Long,
        timeProvider: TimeProvider,
    ): Long {
        // 시간은 그대로 사용하고, 상위 레벨에서 시퀀스로 고유성 보장
        // 실제 시퀀스 조작은 Snowflake 클래스에서 처리
        return lastTimeMillis
    }
}

/**
 * 시계 역행 시 즉시 실패하는 전략 (기본 동작)
 */
class FailStrategy : ClockBackwardStrategy {

    override fun handleClockBackward(
        lastTimeMillis: Long,
        currentTimeMillis: Long,
        timeProvider: TimeProvider,
    ): Long {
        throw ClockBackwardException(lastTimeMillis, currentTimeMillis)
    }
}

/**
 * 시간 제공자 인터페이스 (테스트 가능성을 위한 추상화)
 */
interface TimeProvider {
    fun currentTimeMillis(): Long
}

/**
 * 시스템 시간을 제공하는 기본 구현체
 */
object SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

/**
 * 시계 역행 시 발생하는 예외
 */
class ClockBackwardException(
    val lastTimeMillis: Long,
    val currentTimeMillis: Long,
    message: String? = null,
) : RuntimeException(
    message ?: "Clock moved backwards: last=$lastTimeMillis, current=$currentTimeMillis, diff=${lastTimeMillis - currentTimeMillis}ms"
) {
    val backwardMillis: Long = lastTimeMillis - currentTimeMillis
}