package me.helloc.techwikiplus.service.common.infrastructure.id

import me.helloc.common.snowflake.Snowflake
import me.helloc.techwikiplus.service.document.domain.model.DocumentId
import me.helloc.techwikiplus.service.document.domain.service.port.DocumentIdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakeDocumentIdGenerator(
    private val snowflake: Snowflake
) : DocumentIdGenerator {

    override fun next(): DocumentId {
        return DocumentId(snowflake.nextId())
    }
}
