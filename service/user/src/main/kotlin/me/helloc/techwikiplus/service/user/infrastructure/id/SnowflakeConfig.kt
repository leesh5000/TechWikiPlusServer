package me.helloc.techwikiplus.service.user.infrastructure.id

import me.helloc.common.snowflake.Snowflake
import me.helloc.common.snowflake.SnowflakeConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SnowflakeConfig {
    /**
     * 애플리케이션 전체에서 공유되는 단일 Snowflake 인스턴스를 생성합니다.
     *
     * nodeId는 다음 우선순위로 결정됩니다:
     * 1. 시스템 프로퍼티 'snowflake.node.id'
     * 2. 환경변수 'SNOWFLAKE_NODE_ID'
     * 3. 기본값 0
     */
    @Bean
    fun snowflake(): Snowflake {
        val configuration = SnowflakeConfiguration.fromEnvironment()
        return Snowflake(configuration)
    }
}
