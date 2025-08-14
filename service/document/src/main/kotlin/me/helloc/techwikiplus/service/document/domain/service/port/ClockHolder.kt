package me.helloc.techwikiplus.service.document.domain.service.port

import java.time.Instant

interface ClockHolder {
    fun now(): Instant

    fun nowEpochMilli(): Long

    fun nowEpochSecond(): Int
}
