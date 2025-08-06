package me.helloc.techwikiplus.service.user.domain.port

import java.time.Instant

interface ClockHolder {
    fun now(): Instant

    fun nowEpochMilli(): Long

    fun nowEpochSecond(): Int
}
