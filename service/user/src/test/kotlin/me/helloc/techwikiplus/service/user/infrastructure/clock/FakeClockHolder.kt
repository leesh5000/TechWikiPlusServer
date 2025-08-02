package me.helloc.techwikiplus.service.user.infrastructure.clock

import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import java.time.Instant

class FakeClockHolder(
    private val now: Instant,
) : ClockHolder {
    override fun now(): Instant {
        return now
    }

    override fun nowEpochMilli(): Long {
        return now.toEpochMilli()
    }

    override fun nowEpochSecond(): Int {
        return now.epochSecond.toInt()
    }
}
