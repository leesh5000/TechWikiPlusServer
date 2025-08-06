package me.helloc.techwikiplus.service.user.infrastructure.id

import me.helloc.common.snowflake.Snowflake
import me.helloc.techwikiplus.service.user.domain.port.IdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakeIdGenerator : IdGenerator {
    private val snowflake = Snowflake()

    override fun next(): String {
        return snowflake.nextId().toString()
    }
}
