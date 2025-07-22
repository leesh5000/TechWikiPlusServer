package me.helloc.techwikiplus.user.domain.service

import java.time.LocalDateTime

interface Clock {
    fun currentTimeMillis(): Long

    fun localDateTime(): LocalDateTime

    companion object {
        val system: Clock =
            object : Clock {
                override fun currentTimeMillis(): Long = System.currentTimeMillis()

                override fun localDateTime(): LocalDateTime = LocalDateTime.now()
            }
    }
}
