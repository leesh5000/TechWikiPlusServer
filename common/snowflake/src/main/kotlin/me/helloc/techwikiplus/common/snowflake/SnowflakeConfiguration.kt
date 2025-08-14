package me.helloc.common.snowflake

/**
 * Snowflake ID 생성기의 설정을 관리하는 클래스
 */
data class SnowflakeConfiguration(
    val nodeId: Long,
) {
    companion object {
        private const val MAX_NODE_ID = (1L shl 10) - 1 // 10비트 노드ID의 최대값

        /**
         * 환경변수나 시스템 프로퍼티에서 nodeId를 읽어 설정을 생성합니다.
         *
         * 우선순위:
         * 1. 시스템 프로퍼티 'snowflake.node.id'
         * 2. 환경변수 'SNOWFLAKE_NODE_ID'
         * 3. 기본값 0
         */
        fun fromEnvironment(): SnowflakeConfiguration {
            val nodeId =
                System.getProperty("snowflake.node.id")?.toLongOrNull()
                    ?: System.getenv("SNOWFLAKE_NODE_ID")?.toLongOrNull()
                    ?: 0L

            return SnowflakeConfiguration(nodeId)
        }

        /**
         * nodeId 유효성 검증
         */
        fun validateNodeId(nodeId: Long): Long {
            if (nodeId < 0 || nodeId > MAX_NODE_ID) {
                throw IllegalArgumentException("NodeId must be between 0 and $MAX_NODE_ID, but got $nodeId")
            }
            return nodeId
        }
    }

    init {
        validateNodeId(nodeId)
    }
}
