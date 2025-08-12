package me.helloc.techwikiplus.service.common.infrastructure.id

import me.helloc.common.snowflake.Snowflake
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.service.port.IdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakeIdGenerator : IdGenerator {
    private val snowflake = Snowflake()

    override fun next(): UserId {
        return UserId(snowflake.nextId())
    }
}
