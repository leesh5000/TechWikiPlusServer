package me.helloc.techwikiplus.service.user.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.domain.model.type.UserRole
import me.helloc.techwikiplus.domain.model.type.UserStatus
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.model.value.Nickname
import me.helloc.techwikiplus.domain.model.value.Password
import me.helloc.techwikiplus.domain.service.port.PasswordEncoder
import java.time.Instant

class UserTest : FunSpec({

    val testPasswordEncoder =
        object : PasswordEncoder {
            override fun encode(rawPassword: String): String {
                return "encoded:$rawPassword"
            }

            override fun matches(
                rawPassword: String,
                encodedPassword: String,
            ): Boolean {
                return encodedPassword == encode(rawPassword)
            }
        }

    test("should create user with all required fields") {
        val now = Instant.now()
        val user =
            User(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", testPasswordEncoder),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )

        user.id shouldBe "123456789"
        user.email.value shouldBe "user@example.com"
        user.nickname.value shouldBe "testuser"
        user.password.value shouldBe "encoded:password123"
        user.status shouldBe UserStatus.ACTIVE
        user.role shouldBe UserRole.USER
        user.createdAt shouldBe now
        user.modifiedAt shouldBe now
    }

    test("should create user with default status and role") {
        val now = Instant.now()
        val user =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", testPasswordEncoder),
                createdAt = now,
            )

        user.status shouldBe UserStatus.ACTIVE
        user.role shouldBe UserRole.USER
        user.modifiedAt shouldBe user.createdAt
    }

    test("should be immutable") {
        val user =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", testPasswordEncoder),
                createdAt = Instant.now(),
            )

        // All properties should be val (immutable)
        user.id shouldBe "123456789"
        user.email.value shouldBe "user@example.com"
        user.nickname.value shouldBe "testuser"
    }

    test("should create copy with updated fields") {
        val now = Instant.now()
        val originalUser =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", testPasswordEncoder),
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

    test("should implement equals correctly based on id only") {
        val now = Instant.now()
        val laterTime = now.plusSeconds(3600)

        val user1 =
            User(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", testPasswordEncoder),
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
                password = Password.fromRawPassword("different456", testPasswordEncoder),
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
                password = Password.fromRawPassword("password123", testPasswordEncoder),
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

    test("should implement hashCode correctly based on id only") {
        val now = Instant.now()
        val laterTime = now.plusSeconds(3600)

        val user1 =
            User(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", testPasswordEncoder),
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
                password = Password.fromRawPassword("different456", testPasswordEncoder),
                status = UserStatus.BANNED,
                role = UserRole.ADMIN,
                createdAt = laterTime,
                modifiedAt = laterTime,
            )

        user1.hashCode() shouldBe user2.hashCode() // Same ID = same hashCode
    }

    test("should implement toString correctly") {
        val user =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", testPasswordEncoder),
                createdAt = Instant.now(),
            )

        val toString = user.toString()
        toString shouldNotBe null
        toString.contains("123456789") shouldBe true
        toString.contains("user@example.com") shouldBe true
        toString.contains("password123") shouldBe false // Password should not be exposed
    }

    test("should reject creation with blank id") {
        shouldThrow<IllegalArgumentException> {
            User.create(
                id = "",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", testPasswordEncoder),
                createdAt = Instant.now(),
            )
        }

        shouldThrow<IllegalArgumentException> {
            User.create(
                id = "   ",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", testPasswordEncoder),
                createdAt = Instant.now(),
            )
        }
    }
})
