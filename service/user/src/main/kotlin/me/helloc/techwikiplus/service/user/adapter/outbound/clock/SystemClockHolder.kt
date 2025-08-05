package me.helloc.techwikiplus.service.user.adapter.outbound.clock

import me.helloc.techwikiplus.service.user.application.port.outbound.ClockHolder
import org.springframework.stereotype.Component
import java.time.Instant

@Component
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
