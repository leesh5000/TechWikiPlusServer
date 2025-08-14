package me.helloc.techwikiplus.service.user.infrastructure

import java.time.Instant

class FakeClockHolder(
    private var fixedInstant: Instant = Instant.parse("2025-01-01T00:00:00Z"),
) : me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder,
    me.helloc.techwikiplus.service.document.domain.service.port.ClockHolder {
    override fun now(): Instant {
        return fixedInstant
    }

    override fun nowEpochMilli(): Long {
        return fixedInstant.toEpochMilli()
    }

    override fun nowEpochSecond(): Int {
        return fixedInstant.epochSecond.toInt()
    }

    fun setFixedTime(instant: Instant) {
        this.fixedInstant = instant
    }

    fun advanceTimeBySeconds(seconds: Long) {
        this.fixedInstant = fixedInstant.plusSeconds(seconds)
    }

    fun advanceTimeByMinutes(minutes: Long) {
        this.fixedInstant = fixedInstant.plusSeconds(minutes * 60)
    }
}
