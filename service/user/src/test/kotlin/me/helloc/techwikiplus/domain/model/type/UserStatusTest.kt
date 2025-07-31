package me.helloc.techwikiplus.domain.model.type

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.domain.model.type.UserStatus

class UserStatusTest : FunSpec({

    test("should have all required status values") {
        val statusValues = UserStatus.entries.map { it.name }

        statusValues shouldContainExactlyInAnyOrder
            listOf(
                "ACTIVE",
                "DORMANT",
                "BANNED",
                "PENDING",
                "DELETED",
            )
    }

    test("should return correct value from string") {
        UserStatus.valueOf("ACTIVE") shouldBe UserStatus.ACTIVE
        UserStatus.valueOf("DORMANT") shouldBe UserStatus.DORMANT
        UserStatus.valueOf("BANNED") shouldBe UserStatus.BANNED
        UserStatus.valueOf("PENDING") shouldBe UserStatus.PENDING
        UserStatus.valueOf("DELETED") shouldBe UserStatus.DELETED
    }

    test("should have correct number of status values") {
        UserStatus.entries.size shouldBe 5
    }
})
