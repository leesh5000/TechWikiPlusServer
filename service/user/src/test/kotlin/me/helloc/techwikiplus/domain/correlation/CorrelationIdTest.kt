package me.helloc.techwikiplus.domain.correlation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("CorrelationId 도메인 값 객체 테스트")
class CorrelationIdTest {
    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("유효한 UUID 형식의 문자열로 CorrelationId를 생성할 수 있다")
        fun `should create CorrelationId with valid UUID string`() {
            // given
            val validUuid = "550e8400-e29b-41d4-a716-446655440000"

            // when
            val correlationId = CorrelationId(validUuid)

            // then
            assertEquals(validUuid, correlationId.value)
        }

        @Test
        @DisplayName("빈 문자열로 CorrelationId를 생성하려고 하면 예외가 발생한다")
        fun `should throw exception when creating with empty string`() {
            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    CorrelationId("")
                }
            assertEquals("Correlation ID cannot be blank", exception.message)
        }

        @Test
        @DisplayName("공백 문자열로 CorrelationId를 생성하려고 하면 예외가 발생한다")
        fun `should throw exception when creating with blank string`() {
            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    CorrelationId("   ")
                }
            assertEquals("Correlation ID cannot be blank", exception.message)
        }

        @Test
        @DisplayName("유효하지 않은 UUID 형식으로 생성하려고 하면 예외가 발생한다")
        fun `should throw exception when creating with invalid UUID format`() {
            // given
            val invalidUuid = "not-a-valid-uuid"

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    CorrelationId(invalidUuid)
                }
            assertEquals("Invalid UUID format: $invalidUuid", exception.message)
        }
    }

    @Nested
    @DisplayName("팩토리 메서드 테스트")
    inner class FactoryMethodTest {
        @Test
        @DisplayName("generate 메서드는 새로운 UUID 기반 CorrelationId를 생성한다")
        fun `should generate new CorrelationId with UUID`() {
            // when
            val correlationId = CorrelationId.generate()

            // then
            assertNotNull(correlationId)
            assertNotNull(correlationId.value)
            // UUID 형식 검증 (8-4-4-4-12 패턴)
            assertTrue(
                correlationId.value.matches(
                    Regex(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
                    ),
                ),
            )
        }

        @Test
        @DisplayName("generate 메서드는 매번 다른 CorrelationId를 생성한다")
        fun `should generate different CorrelationIds`() {
            // when
            val id1 = CorrelationId.generate()
            val id2 = CorrelationId.generate()

            // then
            assertNotEquals(id1, id2)
            assertNotEquals(id1.value, id2.value)
        }
    }

    @Nested
    @DisplayName("동등성 테스트")
    inner class EqualityTest {
        @Test
        @DisplayName("동일한 값을 가진 CorrelationId는 동등하다")
        fun `should be equal when values are same`() {
            // given
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
            val id1 = CorrelationId(uuid)
            val id2 = CorrelationId(uuid)

            // then
            assertEquals(id1, id2)
            assertEquals(id1.hashCode(), id2.hashCode())
        }

        @Test
        @DisplayName("다른 값을 가진 CorrelationId는 동등하지 않다")
        fun `should not be equal when values are different`() {
            // given
            val id1 = CorrelationId.generate()
            val id2 = CorrelationId.generate()

            // then
            assertNotEquals(id1, id2)
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    inner class ToStringTest {
        @Test
        @DisplayName("toString은 CorrelationId의 값을 반환한다")
        fun `should return value in toString`() {
            // given
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
            val correlationId = CorrelationId(uuid)

            // when
            val result = correlationId.toString()

            // then
            assertEquals(uuid, result)
        }
    }
}
