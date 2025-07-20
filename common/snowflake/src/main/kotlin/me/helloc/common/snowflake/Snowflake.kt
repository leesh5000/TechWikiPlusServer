package me.helloc.common.snowflake

/**
 * 분산 환경에서 고유한 64비트 ID를 안전하고 효율적으로 생성하는 Snowflake 구현체
 *
 * 구조: [41비트 타임스탬프][10비트 노드ID][12비트 시퀀스]
 * 
 * 주요 특징:
 * - 설정 가능한 NodeId 관리 (환경변수, 고정값, 랜덤)
 * - 시계 역행 복구 전략 선택 가능 (대기, 실패, 시퀀스)
 * - 테스트 가능한 설계 (TimeProvider 추상화)
 * - 높은 성능 (100만+ IDs/sec)
 */
class Snowflake {
    
    private val config: SnowflakeConfig
    

    /**
     * 현재 노드의 ID
     */
    val nodeId: Long

    // 마지막 생성 시간과 시퀀스 (동기화 필요)
    private var lastTimeMillis: Long = 0L
    private var sequence: Long = 0L

    /**
     * 기본 설정으로 Snowflake 인스턴스를 생성한다.
     * 환경변수 SNOWFLAKE_NODE_ID에서 NodeId를 읽어온다.
     */
    constructor() : this(SnowflakeConfig())

    /**
     * 지정된 설정으로 Snowflake 인스턴스를 생성한다.
     */
    constructor(config: SnowflakeConfig) {
        this.config = config
        this.nodeId = config.nodeIdProvider.getNodeId()
        
        // NodeId 유효성 검증
        NodeIdValidator.validateOrThrow(nodeId)
    }

    /**
     * 고정된 NodeId로 Snowflake 인스턴스를 생성한다.
     * 개발 및 테스트 환경에서 사용하기 편리하다.
     */
    constructor(nodeId: Long) : this(
        SnowflakeConfig.Builder()
            .staticNodeId(nodeId)
            .build()
    )

    /**
     * 고유한 ID를 생성한다.
     * 
     * @return 64비트 고유 ID
     * @throws ClockBackwardException 시계 역행을 복구할 수 없는 경우
     */
    @Synchronized
    fun nextId(): Long {
        var currentTime = config.timeProvider.currentTimeMillis()

        // 시계 역행 검사 및 처리
        if (currentTime < lastTimeMillis) {
            currentTime = config.clockBackwardStrategy.handleClockBackward(
                lastTimeMillis = lastTimeMillis,
                currentTimeMillis = currentTime,
                timeProvider = config.timeProvider
            )
        }

        // 동일한 밀리초 내에서 시퀀스 증가
        if (currentTime == lastTimeMillis) {
            sequence = (sequence + 1) and SnowflakeConfig.MAX_SEQUENCE
            
            // 시퀀스 오버플로우 시 다음 밀리초까지 대기
            if (sequence == 0L) {
                currentTime = waitNextMillis(currentTime)
            }
        } else {
            // 새로운 밀리초에서는 시퀀스 초기화
            sequence = 0L
        }

        lastTimeMillis = currentTime

        // ID 조합: [타임스탬프][노드ID][시퀀스]
        return buildId(currentTime)
    }

    /**
     * 타임스탬프, 노드ID, 시퀀스를 조합하여 최종 ID를 생성한다.
     */
    private fun buildId(timeMillis: Long): Long {
        val timestamp = timeMillis - config.epochMillis
        return (timestamp shl TIMESTAMP_LEFT_SHIFT) or
            (nodeId shl NODE_ID_LEFT_SHIFT) or
            sequence
    }

    /**
     * 다음 밀리초가 될 때까지 대기한다.
     */
    private fun waitNextMillis(currentTimestamp: Long): Long {
        var timestamp = currentTimestamp
        while (timestamp <= lastTimeMillis) {
            timestamp = config.timeProvider.currentTimeMillis()
        }
        return timestamp
    }

    /**
     * 현재 Snowflake 인스턴스의 정보를 반환한다.
     */
    fun getInfo(): SnowflakeInfo {
        return SnowflakeInfo(
            nodeId = nodeId,
            epochMillis = config.epochMillis,
            lastGeneratedTimeMillis = lastTimeMillis,
            currentSequence = sequence
        )
    }

    companion object {
        private const val NODE_ID_BITS = 10
        private const val SEQUENCE_BITS = 12
        private const val TIMESTAMP_LEFT_SHIFT = NODE_ID_BITS + SEQUENCE_BITS // 22
        private const val NODE_ID_LEFT_SHIFT = SEQUENCE_BITS // 12
        
        /**
         * 환경변수 기반 Snowflake 인스턴스를 생성한다.
         */
        fun create(): Snowflake = Snowflake()

        /**
         * 지정된 NodeId로 Snowflake 인스턴스를 생성한다.
         */
        fun create(nodeId: Long): Snowflake = Snowflake(nodeId)

        /**
         * 지정된 설정으로 Snowflake 인스턴스를 생성한다.
         */
        fun create(config: SnowflakeConfig): Snowflake = Snowflake(config)
    }
}

/**
 * Snowflake 인스턴스의 현재 상태 정보
 */
data class SnowflakeInfo(
    val nodeId: Long,
    val epochMillis: Long,
    val lastGeneratedTimeMillis: Long,
    val currentSequence: Long,
) {
    /**
     * 마지막 생성 시간부터 현재까지의 경과 시간 (밀리초)
     */
    fun timeSinceLastGeneration(): Long {
        return System.currentTimeMillis() - lastGeneratedTimeMillis
    }

    /**
     * epoch 시작부터 현재까지의 경과 시간 (밀리초)
     */
    fun timeSinceEpoch(): Long {
        return System.currentTimeMillis() - epochMillis
    }
}