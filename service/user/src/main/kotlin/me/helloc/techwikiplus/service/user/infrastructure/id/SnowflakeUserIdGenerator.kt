package me.helloc.techwikiplus.service.user.infrastructure.id

import me.helloc.common.snowflake.Snowflake
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.service.port.UserIdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakeUserIdGenerator(
    private val snowflake: Snowflake,
) : UserIdGenerator {
    override fun next(): UserId {
        return UserId(snowflake.nextId())
    }
}
