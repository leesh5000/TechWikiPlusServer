package me.helloc.techwikiplus.service.user.infrastructure.clock

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class SystemClockHolder(
    private val clock: Clock = Clock.systemDefaultZone(),
) : me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder {
    override fun now(): Instant {
        return Instant.now(clock)
    }

    override fun nowEpochMilli(): Long {
        return now().toEpochMilli()
    }

    override fun nowEpochSecond(): Int {
        return now().epochSecond.toInt()
    }
}
