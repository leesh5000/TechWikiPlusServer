package me.helloc.techwikiplus.user.infrastructure.clock.fake

import me.helloc.techwikiplus.user.domain.port.outbound.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class FakeClock(private var currentTime: LocalDateTime) : Clock {
    override fun currentTimeMillis(): Long = currentTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    override fun localDateTime(): LocalDateTime = currentTime

    fun advanceBy(duration: Duration) {
        currentTime = currentTime.plus(duration)
    }

    fun advanceByDays(days: Long) {
        currentTime = currentTime.plusDays(days)
    }

    fun advanceByHours(hours: Long) {
        currentTime = currentTime.plusHours(hours)
    }

    fun advanceByMinutes(minutes: Long) {
        currentTime = currentTime.plusMinutes(minutes)
    }

    fun setTime(newTime: LocalDateTime) {
        currentTime = newTime
    }
}
