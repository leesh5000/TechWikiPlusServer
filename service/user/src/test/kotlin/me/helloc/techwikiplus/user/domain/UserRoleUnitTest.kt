package me.helloc.techwikiplus.user.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UserRoleUnitTest : FunSpec({

    context("UserRole enum") {
        test("USER 역할이 존재함") {
            val userRole = UserRole.USER
            userRole.name shouldBe "USER"
        }

        test("ADMIN 역할이 존재함") {
            val adminRole = UserRole.ADMIN
            adminRole.name shouldBe "ADMIN"
        }

        test("모든 역할 값을 가져올 수 있음") {
            val roles = UserRole.values()
            roles.size shouldBe 2
            roles.map { it.name } shouldBe listOf("USER", "ADMIN")
        }

        test("문자열로부터 역할을 가져올 수 있음") {
            UserRole.valueOf("USER") shouldBe UserRole.USER
            UserRole.valueOf("ADMIN") shouldBe UserRole.ADMIN
        }
    }
})
