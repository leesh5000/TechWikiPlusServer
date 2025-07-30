package me.helloc.common.snowflake

import kotlin.random.Random

/**
 * 분산 환경에서 고유한 64비트 ID를 생성하는 Snowflake 구현체
 *
 * 구조: [41비트 타임스탬프][10비트 노드ID][12비트 시퀀스]
 */
class Snowflake {
    companion object {
        private const val UNUSED_BITS = 1
        private const val EPOCH_BITS = 41
        private const val NODE_ID_BITS = 10
        private const val SEQUENCE_BITS = 12

        private const val MAX_NODE_ID = (1L shl NODE_ID_BITS) - 1
        private const val MAX_SEQUENCE = (1L shl SEQUENCE_BITS) - 1
    }

    // 임의로 할당된 노드 ID (0..maxNodeId)
    private val nodeId: Long = Random.nextLong(MAX_NODE_ID + 1)

    // 기준 시작 시점 (UTC 2024-01-01T00:00:00Z)
    private val startTimeMillis: Long = 1704067200000L

    // 마지막 할당된 시간, 시퀀스 변수
    private var lastTimeMillis: Long = startTimeMillis
    private var sequence: Long = 0L

    /**
     * 고유한 ID를 생성합니다.
     * @throws IllegalStateException 시스템 시간이 뒤로 돌아간 경우
     */
    @Synchronized
    fun nextId(): Long {
        var currentTime = System.currentTimeMillis()

        if (currentTime < lastTimeMillis) {
            throw IllegalStateException("시스템 시간이 뒤로 이동했습니다. last=$lastTimeMillis, current=$currentTime")
        }

        if (currentTime == lastTimeMillis) {
            sequence = (sequence + 1) and MAX_SEQUENCE
            if (sequence == 0L) {
                currentTime = waitNextMillis(currentTime)
            }
        } else {
            sequence = 0L
        }

        lastTimeMillis = currentTime

        return ((currentTime - startTimeMillis) shl (NODE_ID_BITS + SEQUENCE_BITS)) or
            (nodeId shl SEQUENCE_BITS) or
            sequence
    }

    /**
     * 다음 밀리초가 될 때까지 대기합니다.
     */
    private fun waitNextMillis(currentTimestamp: Long): Long {
        var ts = currentTimestamp
        while (ts <= lastTimeMillis) {
            ts = System.currentTimeMillis()
        }
        return ts
    }
}
