package me.helloc.techwikiplus.service.user.adapter.outbound.clock

import me.helloc.techwikiplus.service.user.application.port.outbound.ClockHolder
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
