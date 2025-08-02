package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import java.time.Instant

class Auditor(
    private val clockHolder: ClockHolder,
) {
    fun generateCreateTime(): Instant {
        return clockHolder.now()
    }

    fun generateModifyTime(): Instant {
        return clockHolder.now()
    }
}
