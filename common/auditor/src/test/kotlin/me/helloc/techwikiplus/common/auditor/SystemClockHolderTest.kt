package me.helloc.techwikiplus.common.auditor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SystemClockHolderTest : FunSpec({

    val sut = SystemClockHolder()

        test("should return the current system time in milliseconds") {
            val currentTime = System.currentTimeMillis()
            val clockTime = sut.nowEpochMilli()
            clockTime shouldBe currentTime
        }

        test("should return the current system time in instant") {
            val currentInstant = System.currentTimeMillis()
            val clockInstant = sut.now()
            clockInstant.toEpochMilli() shouldBe currentTime
        }
    },
)
