package me.helloc.common.snowflake

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class ClockBackwardStrategyTest : FunSpec({

    context("WaitStrategy") {
        test("시계가 정상적으로 돌아올 때까지 대기한다") {
            // Given
            val strategy = WaitStrategy(maxWaitTimeMillis = 1000L)
            val timeProvider = MockTimeProvider()
            val lastTime = 1000L
            val backwardTime = 900L

            // 시간이 역행했다가 정상으로 돌아오는 시나리오
            timeProvider.setTimes(backwardTime, backwardTime, backwardTime, 1001L)

            // When
            val result = strategy.handleClockBackward(lastTime, backwardTime, timeProvider)

            // Then
            result shouldBeGreaterThan lastTime
        }

        test("최대 대기 시간 초과 시 예외 발생") {
            // Given
            val strategy = WaitStrategy(maxWaitTimeMillis = 100L)
            val timeProvider = MockTimeProvider()
            val lastTime = 1000L
            val backwardTime = 900L

            // 계속 역행된 시간만 반환
            timeProvider.setRepeatingTime(backwardTime)

            // When & Then
            shouldThrow<ClockBackwardException> {
                strategy.handleClockBackward(lastTime, backwardTime, timeProvider)
            }
        }
    }

    context("SequenceStrategy") {
        test("시퀀스를 증가시켜 고유성을 보장한다") {
            // Given
            val strategy = SequenceStrategy()
            val timeProvider = MockTimeProvider()
            val lastTime = 1000L
            val backwardTime = 900L

            // When
            val result = strategy.handleClockBackward(lastTime, backwardTime, timeProvider)

            // Then
            result shouldBe lastTime // 시간은 그대로
        }

        test("시계 역행 시 마지막 시간을 반환한다") {
            // Given
            val strategy = SequenceStrategy()
            val timeProvider = MockTimeProvider()
            val lastTime = 1000L
            val backwardTime = 900L

            // When
            val result = strategy.handleClockBackward(lastTime, backwardTime, timeProvider)

            // Then
            result shouldBe lastTime
        }
    }

    context("FailStrategy") {
        test("시계 역행 시 즉시 예외 발생") {
            // Given
            val strategy = FailStrategy()
            val timeProvider = MockTimeProvider()
            val lastTime = 1000L
            val backwardTime = 900L

            // When & Then
            shouldThrow<ClockBackwardException> {
                strategy.handleClockBackward(lastTime, backwardTime, timeProvider)
            }
        }
    }

    context("ClockBackwardException") {
        test("적절한 메시지와 정보를 포함한다") {
            // Given
            val lastTime = 1000L
            val currentTime = 900L
            val diff = lastTime - currentTime

            // When
            val exception = ClockBackwardException(lastTime, currentTime)

            // Then
            exception.message shouldBe "Clock moved backwards: last=$lastTime, current=$currentTime, diff=${diff}ms"
            exception.lastTimeMillis shouldBe lastTime
            exception.currentTimeMillis shouldBe currentTime
            exception.backwardMillis shouldBe diff
        }
    }
})

/**
 * 테스트용 시간 제공자
 */
class MockTimeProvider : TimeProvider {
    private val times = mutableListOf<Long>()
    private var index = 0
    private var repeatingTime: Long? = null

    fun setTimes(vararg times: Long) {
        this.times.clear()
        this.times.addAll(times.toList())
        this.index = 0
        this.repeatingTime = null
    }

    fun setRepeatingTime(time: Long) {
        this.repeatingTime = time
        this.times.clear()
        this.index = 0
    }

    override fun currentTimeMillis(): Long {
        return repeatingTime ?: run {
            if (index < times.size) {
                times[index++]
            } else {
                times.lastOrNull() ?: System.currentTimeMillis()
            }
        }
    }
}
