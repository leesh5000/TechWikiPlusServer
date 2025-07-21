package me.helloc.techwikiplus.user.infrastructure.verificationcode

import me.helloc.common.snowflake.Snowflake
import me.helloc.techwikiplus.user.infrastructure.id.snowflake.SnowflakeIdGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SnowflakeIdGeneratorTest {

    @Test
    fun `generate는 Snowflake의 nextId를 문자열로 반환한다`() {
        // given
        val snowflake = mock<Snowflake>()
        val expectedId = 1234567890L
        whenever(snowflake.nextId()).thenReturn(expectedId)

        val idGenerator = SnowflakeIdGenerator(snowflake)

        // when
        val generatedId = idGenerator.next()

        // then
        assertEquals(expectedId.toString(), generatedId)
    }

    @Test
    fun `generate는 매번 다른 ID를 생성한다`() {
        // given
        val snowflake = mock<Snowflake>()
        val firstId = 1234567890L
        val secondId = 1234567891L
        whenever(snowflake.nextId())
            .thenReturn(firstId)
            .thenReturn(secondId)

        val idGenerator = SnowflakeIdGenerator(snowflake)

        // when
        val generatedId1 = idGenerator.next()
        val generatedId2 = idGenerator.next()

        // then
        assertAll(
            { assertEquals(firstId.toString(), generatedId1) },
            { assertEquals(secondId.toString(), generatedId2) },
            { assertTrue(generatedId1 != generatedId2) }
        )
    }

    @Test
    fun `generate는 null이 아닌 값을 반환한다`() {
        // given
        val snowflake = mock<Snowflake>()
        whenever(snowflake.nextId()).thenReturn(1L)

        val idGenerator = SnowflakeIdGenerator(snowflake)

        // when
        val generatedId = idGenerator.next()

        // then
        assertNotNull(generatedId)
    }
}
