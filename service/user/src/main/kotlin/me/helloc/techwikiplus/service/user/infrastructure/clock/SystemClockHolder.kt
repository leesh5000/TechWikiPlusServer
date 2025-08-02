package me.helloc.techwikiplus.service.user.infrastructure.clock

import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import java.time.Instant

class SystemClockHolder : ClockHolder {
    override fun now(): Instant {
        return Instant.now()
    }

    override fun nowEpochMilli(): Long {
        return System.currentTimeMillis()
    }

    override fun nowEpochSecond(): Int {
        return Instant.now().epochSecond.toInt()
    }
}
