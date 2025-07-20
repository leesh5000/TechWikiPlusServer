package me.helloc.common.snowflake

/**
 * Snowflake NodeId를 제공하는 인터페이스
 * 분산 환경에서 고유한 노드 식별자를 안전하게 제공한다.
 */
interface NodeIdProvider {
    /**
     * 현재 노드의 고유 ID를 반환한다.
     * @return 0-1023 범위의 노드 ID
     * @throws InvalidNodeIdException 유효하지 않은 노드 ID인 경우
     */
    fun getNodeId(): Long
}

/**
 * 환경변수에서 nodeId를 가져오는 Provider
 */
class EnvironmentNodeIdProvider(
    private val environment: Map<String, String> = System.getenv(),
    private val envKey: String = "SNOWFLAKE_NODE_ID",
) : NodeIdProvider {

    override fun getNodeId(): Long {
        val nodeIdStr = environment[envKey]
            ?: throw InvalidNodeIdException("Environment variable '$envKey' not found")

        val nodeId = try {
            nodeIdStr.toLong()
        } catch (e: NumberFormatException) {
            throw InvalidNodeIdException("Invalid nodeId format: $nodeIdStr", e)
        }

        if (!NodeIdValidator.validate(nodeId)) {
            throw InvalidNodeIdException("NodeId $nodeId is out of range (0-1023)")
        }

        return nodeId
    }
}

/**
 * 랜덤하게 생성된 고정 nodeId를 제공하는 Provider
 * 주의: 분산 환경에서는 충돌 위험이 있으므로 테스트 용도로만 사용
 */
class RandomNodeIdProvider(
    private val seed: Long? = null,
) : NodeIdProvider {
    
    private val _nodeId: Long by lazy {
        val random = seed?.let { kotlin.random.Random(it) } ?: kotlin.random.Random
        random.nextLong(NodeIdValidator.MAX_NODE_ID + 1)
    }

    override fun getNodeId(): Long = _nodeId
}

/**
 * 고정된 nodeId를 제공하는 Provider
 */
class StaticNodeIdProvider(
    private val nodeId: Long,
) : NodeIdProvider {

    init {
        if (!NodeIdValidator.validate(nodeId)) {
            throw InvalidNodeIdException("Invalid nodeId: $nodeId (must be 0-1023)")
        }
    }

    override fun getNodeId(): Long = nodeId
}

/**
 * NodeId 유효성 검증 유틸리티
 */
object NodeIdValidator {
    const val MAX_NODE_ID = (1L shl 10) - 1 // 1023

    /**
     * NodeId가 유효한 범위(0-1023)인지 확인한다.
     */
    fun validate(nodeId: Long): Boolean = nodeId in 0..MAX_NODE_ID

    /**
     * NodeId를 검증하고 유효하지 않으면 예외를 발생시킨다.
     */
    fun validateOrThrow(nodeId: Long) {
        if (!validate(nodeId)) {
            throw InvalidNodeIdException("NodeId $nodeId is out of range (0-$MAX_NODE_ID)")
        }
    }
}

/**
 * NodeId 관련 예외
 */
class InvalidNodeIdException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)