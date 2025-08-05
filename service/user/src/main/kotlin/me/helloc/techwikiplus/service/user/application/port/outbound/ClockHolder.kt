package me.helloc.techwikiplus.service.user.application.port.outbound

import java.time.Instant

interface ClockHolder {
    fun now(): Instant

    fun nowEpochMilli(): Long

    fun nowEpochSecond(): Int
}
