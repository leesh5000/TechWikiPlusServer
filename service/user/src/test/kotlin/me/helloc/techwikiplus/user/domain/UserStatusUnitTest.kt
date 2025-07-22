package me.helloc.techwikiplus.user.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UserStatusUnitTest : FunSpec({

    context("UserStatus enum") {
        test("ACTIVE 상태가 존재함") {
            val status = UserStatus.ACTIVE
            status.name shouldBe "ACTIVE"
        }

        test("PENDING 상태가 존재함") {
            val status = UserStatus.PENDING
            status.name shouldBe "PENDING"
        }

        test("BANNED 상태가 존재함") {
            val status = UserStatus.BANNED
            status.name shouldBe "BANNED"
        }

        test("DORMANT 상태가 존재함") {
            val status = UserStatus.DORMANT
            status.name shouldBe "DORMANT"
        }

        test("DELETED 상태가 존재함") {
            val status = UserStatus.DELETED
            status.name shouldBe "DELETED"
        }

        test("모든 상태 값을 가져올 수 있음") {
            val statuses = UserStatus.values()
            statuses.size shouldBe 5
            statuses.map { it.name } shouldBe listOf("ACTIVE", "PENDING", "BANNED", "DORMANT", "DELETED")
        }

        test("문자열로부터 상태를 가져올 수 있음") {
            UserStatus.valueOf("ACTIVE") shouldBe UserStatus.ACTIVE
            UserStatus.valueOf("PENDING") shouldBe UserStatus.PENDING
            UserStatus.valueOf("BANNED") shouldBe UserStatus.BANNED
            UserStatus.valueOf("DORMANT") shouldBe UserStatus.DORMANT
            UserStatus.valueOf("DELETED") shouldBe UserStatus.DELETED
        }
    }
})
