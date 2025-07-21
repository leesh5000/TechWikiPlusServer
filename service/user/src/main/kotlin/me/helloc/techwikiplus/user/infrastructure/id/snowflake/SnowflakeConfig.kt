package me.helloc.techwikiplus.user.infrastructure.id.snowflake

import me.helloc.common.snowflake.Snowflake
import me.helloc.common.snowflake.SnowflakeConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Snowflake ID 생성기 설정
 *
 * application.yml의 snowflake.node-id 설정을 사용하여 노드 ID를 설정한다.
 * 설정이 없으면 환경변수 SNOWFLAKE_NODE_ID를 확인하고, 그것도 없으면 기본값 1을 사용한다.
 * 시계 역행 시 최대 5초까지 대기하여 가용성을 보장한다.
 */
@Configuration
open class SnowflakeConfiguration {

    @Bean
    open fun snowflake(): Snowflake {
        val config = SnowflakeConfig.Builder()
            .environmentNodeId()
            .waitOnClockBackward(5000L)
            .build()
        return Snowflake(config)
    }
}
