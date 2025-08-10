package me.helloc.techwikiplus.service.user.domain.port

import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import java.util.concurrent.atomic.AtomicLong

class FakeIdGenerator(
    private val prefix: String = "test-user-",
    private val startFrom: Long = 1L,
) : IdGenerator {
    private val counter = AtomicLong(startFrom)

    override fun next(): UserId {
        return UserId("$prefix${counter.getAndIncrement()}")
    }

    fun reset(value: Long = 1L) {
        counter.set(value)
    }

    fun setNext(value: String): UserId {
        return UserId(value)
    }
}
