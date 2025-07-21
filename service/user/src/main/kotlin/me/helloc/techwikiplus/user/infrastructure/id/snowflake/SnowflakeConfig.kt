package me.helloc.techwikiplus.user.infrastructure.id.snowflake

import me.helloc.common.snowflake.Snowflake
import me.helloc.common.snowflake.SnowflakeConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class SnowflakeConfiguration {

    @Bean
    open fun snowflake(): Snowflake {
        val config = SnowflakeConfig.Builder()
            .randomNodeId()
            .waitOnClockBackward(5000L)
            .build()
        return Snowflake(config)
    }
}
