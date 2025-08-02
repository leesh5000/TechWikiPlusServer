package me.helloc.techwikiplus.service.user.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordEncoder
import java.time.Instant

class UserTest : FunSpec({

    val testPasswordEncoder = FakePasswordEncoder()

    test("모든 필수 필드를 가진 사용자를 생성해야 한다") {
        val now = Instant.now()
        val user =
            User(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        user.id shouldBe "123456789"
        user.email.value shouldBe "user@example.com"
        user.nickname.value shouldBe "testuser"
        user.encodedPassword.value shouldBe "FAKE_ENCODED:password123"
        user.status shouldBe UserStatus.ACTIVE
        user.role shouldBe UserRole.USER
        user.createdAt shouldBe now
        user.modifiedAt shouldBe now
    }

    test("기본 상태와 역할로 PENDING 상태인 사용자를 생성해야 한다") {
        val now = Instant.now()
        val user =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                createdAt = now,
            )

        user.status shouldBe UserStatus.PENDING
        user.role shouldBe UserRole.USER
        user.modifiedAt shouldBe user.createdAt
    }

    test("불변 객체여야 한다") {
        val user =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                createdAt = Instant.now(),
            )

        // All properties should be val (immutable)
        user.id shouldBe "123456789"
        user.email.value shouldBe "user@example.com"
        user.nickname.value shouldBe "testuser"
    }

    test("업데이트된 필드로 복사본을 생성해야 한다") {
        val now = Instant.now()
        val originalUser =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                createdAt = now,
            )

        val laterTime = now.plusSeconds(1)
        val updatedUser =
            originalUser.copy(
                nickname = Nickname("newNickname"),
                modifiedAt = laterTime,
            )

        updatedUser.id shouldBe originalUser.id
        updatedUser.email shouldBe originalUser.email
        updatedUser.nickname.value shouldBe "newNickname"
        updatedUser.nickname shouldNotBe originalUser.nickname
        updatedUser.modifiedAt shouldNotBe originalUser.modifiedAt
        updatedUser.modifiedAt shouldBe laterTime
    }

    test("ID만으로 equals를 올바르게 구현해야 한다") {
        val now = Instant.now()
        val laterTime = now.plusSeconds(3600)

        val user1 =
            User(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        // Same ID but different fields
        val user2 =
            User(
                id = "123456789",
                email = Email("different@example.com"),
                nickname = Nickname("differentuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                status = UserStatus.BANNED,
                role = UserRole.ADMIN,
                createdAt = laterTime,
                modifiedAt = laterTime,
            )

        // Different ID
        val user3 =
            User(
                id = "987654321",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        user1 shouldBe user2 // Same ID = equal
        user1 shouldNotBe user3 // Different ID = not equal
        user1 shouldNotBe null
        user1 shouldNotBe "user"
    }

    test("ID만으로 hashCode를 올바르게 구현해야 한다") {
        val now = Instant.now()
        val laterTime = now.plusSeconds(3600)

        val user1 =
            User(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        // Same ID but different fields
        val user2 =
            User(
                id = "123456789",
                email = Email("different@example.com"),
                nickname = Nickname("differentuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                status = UserStatus.BANNED,
                role = UserRole.ADMIN,
                createdAt = laterTime,
                modifiedAt = laterTime,
            )

        user1.hashCode() shouldBe user2.hashCode() // Same ID = same hashCode
    }

    test("toString을 올바르게 구현해야 한다") {
        val user =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                createdAt = Instant.now(),
            )

        val toString = user.toString()
        toString shouldNotBe null
        toString.contains("123456789") shouldBe true
        toString.contains("user@example.com") shouldBe true
        toString.contains("password123") shouldBe false // Password should not be exposed
    }

    test("빈 ID로 생성을 거부해야 한다") {
        shouldThrow<IllegalArgumentException> {
            User.create(
                id = "",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                createdAt = Instant.now(),
            )
        }

        shouldThrow<IllegalArgumentException> {
            User.create(
                id = "   ",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(testPasswordEncoder.encode("password123")),
                createdAt = Instant.now(),
            )
        }
    }
})
