package me.helloc.common.snowflake

/**
 * Snowflake ID 생성기의 설정을 관리하는 클래스
 */
data class SnowflakeConfig(
    val nodeIdProvider: NodeIdProvider = EnvironmentNodeIdProvider(),
    val epochMillis: Long = DEFAULT_EPOCH,
    val clockBackwardStrategy: ClockBackwardStrategy = WaitStrategy(),
    val timeProvider: TimeProvider = SystemTimeProvider,
) {
    init {
        require(epochMillis >= 0) { "Epoch must be non-negative: $epochMillis" }
        require(epochMillis <= System.currentTimeMillis()) {
            "Epoch cannot be in the future: $epochMillis > ${System.currentTimeMillis()}"
        }
    }

    companion object {
        /**
         * 기본 epoch: 2024-01-01 00:00:00 UTC
         */
        const val DEFAULT_EPOCH = 1704067200000L

        /**
         * 최대 노드 ID (10비트)
         */
        const val MAX_NODE_ID = (1L shl 10) - 1 // 1023

        /**
         * 최대 시퀀스 번호 (12비트)
         */
        const val MAX_SEQUENCE = (1L shl 12) - 1 // 4095
    }

    /**
     * SnowflakeConfig를 생성하기 위한 빌더 클래스
     */
    class Builder {
        private var nodeIdProvider: NodeIdProvider? = null
        private var epochMillis: Long = DEFAULT_EPOCH
        private var clockBackwardStrategy: ClockBackwardStrategy = WaitStrategy()
        private var timeProvider: TimeProvider = SystemTimeProvider

        /**
         * NodeIdProvider를 직접 설정한다.
         */
        fun nodeIdProvider(provider: NodeIdProvider): Builder {
            this.nodeIdProvider = provider
            return this
        }

        /**
         * 환경변수에서 NodeId를 가져오도록 설정한다.
         */
        fun environmentNodeId(
            environment: Map<String, String> = System.getenv(),
            envKey: String = "SNOWFLAKE_NODE_ID",
        ): Builder {
            this.nodeIdProvider = EnvironmentNodeIdProvider(environment, envKey)
            return this
        }

        /**
         * 고정된 NodeId를 사용하도록 설정한다.
         */
        fun staticNodeId(nodeId: Long): Builder {
            this.nodeIdProvider = StaticNodeIdProvider(nodeId)
            return this
        }

        /**
         * 랜덤 NodeId를 사용하도록 설정한다.
         * 주의: 분산 환경에서는 충돌 위험이 있음
         */
        fun randomNodeId(seed: Long? = null): Builder {
            this.nodeIdProvider = RandomNodeIdProvider(seed)
            return this
        }

        /**
         * Epoch 시작 시점을 설정한다.
         */
        fun epochMillis(epochMillis: Long): Builder {
            this.epochMillis = epochMillis
            return this
        }

        /**
         * 시계 역행 시 대기하는 전략을 설정한다.
         */
        fun waitOnClockBackward(maxWaitTimeMillis: Long = 5000L): Builder {
            this.clockBackwardStrategy = WaitStrategy(maxWaitTimeMillis)
            return this
        }

        /**
         * 시계 역행 시 즉시 실패하는 전략을 설정한다.
         */
        fun failOnClockBackward(): Builder {
            this.clockBackwardStrategy = FailStrategy()
            return this
        }

        /**
         * 시계 역행 시 시퀀스를 사용하는 전략을 설정한다.
         */
        fun useSequenceOnClockBackward(): Builder {
            this.clockBackwardStrategy = SequenceStrategy()
            return this
        }

        /**
         * 시계 역행 처리 전략을 직접 설정한다.
         */
        fun clockBackwardStrategy(strategy: ClockBackwardStrategy): Builder {
            this.clockBackwardStrategy = strategy
            return this
        }

        /**
         * 시간 제공자를 설정한다. (주로 테스트 용도)
         */
        fun timeProvider(provider: TimeProvider): Builder {
            this.timeProvider = provider
            return this
        }

        /**
         * 설정을 기반으로 SnowflakeConfig를 생성한다.
         */
        fun build(): SnowflakeConfig {
            return SnowflakeConfig(
                nodeIdProvider = nodeIdProvider ?: EnvironmentNodeIdProvider(),
                epochMillis = epochMillis,
                clockBackwardStrategy = clockBackwardStrategy,
                timeProvider = timeProvider,
            )
        }
    }
}
