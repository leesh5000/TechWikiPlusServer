package me.helloc.techwikiplus.service.user.domain.model.type

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class UserRoleTest : FunSpec({

    test("should have all required role values") {
        val roleValues = UserRole.entries.map { it.name }

        roleValues shouldContainExactlyInAnyOrder
            listOf(
                "ADMIN",
                "USER",
            )
    }

    test("should return correct value from string") {
        UserRole.valueOf("ADMIN") shouldBe UserRole.ADMIN
        UserRole.valueOf("USER") shouldBe UserRole.USER
    }

    test("should have correct number of role values") {
        UserRole.entries.size shouldBe 2
    }
})
