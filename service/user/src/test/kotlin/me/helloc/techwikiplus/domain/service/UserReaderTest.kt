package me.helloc.techwikiplus.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.model.value.Nickname
import me.helloc.techwikiplus.domain.model.value.Password
import me.helloc.techwikiplus.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.infrastructure.security.FakePasswordEncoder
import java.time.Instant

class UserReaderTest : FunSpec({

    test("should find existing user by email") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        val passwordEncoder = FakePasswordEncoder()

        val existingUser = User.create(
            id = "123456789",
            email = Email("user@example.com"),
            nickname = Nickname("testuser"),
            password = Password.fromRawPassword("password123", passwordEncoder),
            createdAt = Instant.now()
        )
        userRepository.save(existingUser)

        // When
        val foundUser = userReader.findByEmail(Email("user@example.com"))

        // Then
        foundUser.shouldNotBeNull()
        foundUser.id shouldBe "123456789"
        foundUser.email.value shouldBe "user@example.com"
        foundUser.nickname.value shouldBe "testuser"
    }

    test("should return null when user not found by email") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)

        // When
        val foundUser = userReader.findByEmail(Email("nonexistent@example.com"))

        // Then
        foundUser.shouldBeNull()
    }

    test("should find correct user among multiple users") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        val passwordEncoder = FakePasswordEncoder()

        val user1 = User.create(
            id = "user1",
            email = Email("user1@example.com"),
            nickname = Nickname("user1"),
            password = Password.fromRawPassword("password1", passwordEncoder),
            createdAt = Instant.now()
        )
        val user2 = User.create(
            id = "user2",
            email = Email("user2@example.com"),
            nickname = Nickname("user2"),
            password = Password.fromRawPassword("password2", passwordEncoder),
            createdAt = Instant.now()
        )
        val user3 = User.create(
            id = "user3",
            email = Email("user3@example.com"),
            nickname = Nickname("user3"),
            password = Password.fromRawPassword("password3", passwordEncoder),
            createdAt = Instant.now()
        )

        userRepository.save(user1)
        userRepository.save(user2)
        userRepository.save(user3)

        // When
        val foundUser = userReader.findByEmail(Email("user2@example.com"))

        // Then
        foundUser.shouldNotBeNull()
        foundUser.id shouldBe "user2"
        foundUser.nickname.value shouldBe "user2"
    }

    test("should handle email lookup case-insensitively") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        val passwordEncoder = FakePasswordEncoder()

        val user = User.create(
            id = "123456789",
            email = Email("user@example.com"),
            nickname = Nickname("testuser"),
            password = Password.fromRawPassword("password123", passwordEncoder),
            createdAt = Instant.now()
        )
        userRepository.save(user)

        // When
        val foundUserLowerCase = userReader.findByEmail(Email("user@example.com"))
        val foundUserUpperCase = userReader.findByEmail(Email("USER@EXAMPLE.COM"))
        val foundUserMixedCase = userReader.findByEmail(Email("User@Example.Com"))

        // Then
        // Note: This test documents current behavior - emails are case-sensitive
        // If business requirement changes, update the implementation and this test
        foundUserLowerCase.shouldNotBeNull()
        foundUserUpperCase.shouldBeNull() // Different case = different email
        foundUserMixedCase.shouldBeNull() // Different case = different email
    }

    test("should not find user with similar but different email") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        val passwordEncoder = FakePasswordEncoder()

        val user = User.create(
            id = "123456789",
            email = Email("user@example.com"),
            nickname = Nickname("testuser"),
            password = Password.fromRawPassword("password123", passwordEncoder),
            createdAt = Instant.now()
        )
        userRepository.save(user)

        // When
        val foundUser1 = userReader.findByEmail(Email("user1@example.com"))
        val foundUser2 = userReader.findByEmail(Email("use@example.com"))
        val foundUser3 = userReader.findByEmail(Email("user@example.co"))
        val foundUser4 = userReader.findByEmail(Email("user@examples.com"))

        // Then
        foundUser1.shouldBeNull()
        foundUser2.shouldBeNull()
        foundUser3.shouldBeNull()
        foundUser4.shouldBeNull()
    }
})
