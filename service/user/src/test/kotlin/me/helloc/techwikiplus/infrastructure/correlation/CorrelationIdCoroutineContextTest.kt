package me.helloc.techwikiplus.infrastructure.correlation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.UUID

@DisplayName("CorrelationIdCoroutineContext 테스트")
class CorrelationIdCoroutineContextTest {
    private val correlationId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        MDC.clear()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    @DisplayName("코루틴 컨텍스트에서 Correlation ID가 전파된다")
    fun `should propagate correlation id in coroutine context`() =
        runBlocking {
            // given
            MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId)

            // when
            withContext(Dispatchers.IO + correlationIdContext()) {
                // then - 다른 스레드에서도 correlation ID가 유지됨
                val mdcValue = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
                assertEquals(correlationId, mdcValue)
            }
        }

    @Test
    @DisplayName("중첩된 코루틴에서 Correlation ID가 전파된다")
    fun `should propagate correlation id in nested coroutines`() =
        runBlocking {
            // given
            MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId)

            // when
            withContext(Dispatchers.Default + correlationIdContext()) {
                launch {
                    delay(10)
                    // then - 자식 코루틴에서도 correlation ID가 유지됨
                    val mdcValue = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
                    assertEquals(correlationId, mdcValue)
                }

                async {
                    delay(10)
                    // then - async 블록에서도 correlation ID가 유지됨
                    MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
                }.await().also { mdcValue ->
                    assertEquals(correlationId, mdcValue)
                }
            }
        }

    @Test
    @DisplayName("코루틴 컨텍스트가 끝나면 원래 MDC 값이 복원된다")
    fun `should restore original MDC value after coroutine context`() =
        runBlocking {
            // given
            val originalValue = "original-correlation-id"
            MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, originalValue)

            // when
            withContext(Dispatchers.IO + CorrelationIdCoroutineContext(correlationId)) {
                // 코루틴 내부에서는 새로운 correlation ID
                assertEquals(correlationId, MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
            }

            // then - 코루틴이 끝나면 원래 값으로 복원됨
            assertEquals(originalValue, MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
        }

    @Test
    @DisplayName("MDC에 Correlation ID가 없을 때도 정상 동작한다")
    fun `should handle null correlation id`() =
        runBlocking {
            // given - MDC에 correlation ID가 없음
            assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))

            // when
            withContext(Dispatchers.IO + correlationIdContext()) {
                // then - 여전히 null
                assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
            }
        }

    @Test
    @DisplayName("MDCContext와 함께 사용할 때 모든 MDC 값이 전파된다")
    fun `should propagate all MDC values with MDCContext`() =
        runBlocking {
            // given
            MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId)
            MDC.put("userId", "user123")
            MDC.put("requestId", "req456")

            // when
            withContext(Dispatchers.IO + mdcContextWithCorrelationId()) {
                // then - 모든 MDC 값이 전파됨
                assertEquals(correlationId, MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
                assertEquals("user123", MDC.get("userId"))
                assertEquals("req456", MDC.get("requestId"))
            }
        }

    @Test
    @DisplayName("여러 스레드에서 동시에 실행되는 코루틴에서 각각의 Correlation ID가 유지된다")
    fun `should maintain separate correlation ids in concurrent coroutines`() =
        runBlocking {
            val correlationId1 = UUID.randomUUID().toString()
            val correlationId2 = UUID.randomUUID().toString()

            // when - 두 개의 코루틴이 서로 다른 correlation ID로 동시에 실행
            val job1 =
                async(Dispatchers.IO + CorrelationIdCoroutineContext(correlationId1)) {
                    delay(50)
                    MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
                }

            val job2 =
                async(Dispatchers.IO + CorrelationIdCoroutineContext(correlationId2)) {
                    delay(50)
                    MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
                }

            // then - 각각의 correlation ID가 올바르게 유지됨
            assertEquals(correlationId1, job1.await())
            assertEquals(correlationId2, job2.await())
        }
}
