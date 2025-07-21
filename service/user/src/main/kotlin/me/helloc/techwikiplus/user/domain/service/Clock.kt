package me.helloc.techwikiplus.user.domain.service

import java.time.LocalDateTime

interface ClockHolder {
    fun now(): Long
    fun localDateTime(): LocalDateTime
}
