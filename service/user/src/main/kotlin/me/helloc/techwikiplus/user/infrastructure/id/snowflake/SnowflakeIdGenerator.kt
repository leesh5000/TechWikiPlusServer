package me.helloc.techwikiplus.user.infrastructure.id.snowflake

import me.helloc.common.snowflake.Snowflake
import me.helloc.techwikiplus.user.domain.service.IdGenerator
import org.springframework.stereotype.Component

/**
 * Snowflake 기반 ID 생성기 구현체
 *
 * Snowflake 알고리즘을 사용하여 분산 환경에서도 고유한 ID를 생성한다.
 * 도메인 계층의 IdGenerator 인터페이스를 구현하여 인프라 계층에 위치한다.
 */
@Component
class SnowflakeIdGenerator(
    private val snowflake: Snowflake
) : IdGenerator {

    override fun next(): Long {
        return snowflake.nextId()
    }
}
