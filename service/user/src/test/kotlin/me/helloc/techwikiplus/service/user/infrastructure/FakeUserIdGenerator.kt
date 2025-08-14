package me.helloc.techwikiplus.service.user.infrastructure

import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.service.port.UserIdGenerator
import java.util.concurrent.atomic.AtomicLong

class FakeUserIdGenerator(
    // Snowflake ID처럼 큰 숫자로 시작
    startFrom: Long = 1000000L,
) : UserIdGenerator {
    private val counter = AtomicLong(startFrom)

    override fun next(): UserId {
        return UserId(counter.getAndIncrement())
    }

    fun reset(value: Long = 1000000L) {
        counter.set(value)
    }

    fun setNext(value: Long): UserId {
        return UserId(value)
    }
}
