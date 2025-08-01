package me.helloc.techwikiplus.common.auditor

import java.time.Instant

class SystemClockHolder {
    fun now(): Instant {
        return Instant.now()
    }

    fun nowEpochMilli(): Long {
        return System.currentTimeMillis()
    }
}
