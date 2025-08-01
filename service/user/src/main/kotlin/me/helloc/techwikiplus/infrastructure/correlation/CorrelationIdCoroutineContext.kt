package me.helloc.techwikiplus.infrastructure.correlation

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import org.slf4j.MDC
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context element for propagating Correlation ID in MDC
 *
 * This ensures that the Correlation ID is properly propagated
 * across coroutine context switches and thread boundaries.
 */
class CorrelationIdCoroutineContext(
    private val correlationId: String?,
) : ThreadContextElement<String?>, AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CorrelationIdCoroutineContext> {
        private val logger = KotlinLogging.logger {}
    }

    override fun updateThreadContext(context: CoroutineContext): String? {
        val oldValue = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
        if (correlationId != null) {
            MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId)
        } else {
            MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
        }
        return oldValue
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: String?,
    ) {
        if (oldState != null) {
            MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, oldState)
        } else {
            MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
        }
    }
}

/**
 * Extension function to create a coroutine context with the current Correlation ID
 *
 * Usage:
 * ```
 * withContext(currentCoroutineContext() + correlationIdContext()) {
 *     // Correlation ID will be propagated here
 * }
 * ```
 */
fun correlationIdContext(): CoroutineContext {
    val correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
    return CorrelationIdCoroutineContext(correlationId)
}

/**
 * Extension function to combine MDCContext with CorrelationIdContext
 *
 * This ensures all MDC values including correlation ID are propagated
 */
fun mdcContextWithCorrelationId(): CoroutineContext = MDCContext() + correlationIdContext()
