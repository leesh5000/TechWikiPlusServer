package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.infrastructure.clock.FakeClockHolder
import java.time.Clock
import java.time.Instant

class AuditorUnitTest : FunSpec(
    {

        val now: Instant = Clock.systemDefaultZone().instant()
        val clockHolder =
            FakeClockHolder(
                now = now,
            )

        test("생성일 데이터를 생성한다") {
            // Given
            val auditor = Auditor(clockHolder)

            // When
            val createdAt = auditor.generateCreateTime()

            // Then
            createdAt shouldBe now
        }

        test("수정일 데이터를 생성한다") {
            // Given
            val auditor = Auditor(clockHolder)

            // When
            val modifiedAt = auditor.generateModifyTime()

            // Then
            modifiedAt shouldBe now
        }
    },
)
