package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordEncoder

class UserPasswordServiceTest : FunSpec({

    test("should encode raw password using password encoder") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = "mySecretPassword123"

        // When
        val encodedPassword = userPasswordService.encode(rawPassword)

        // Then
        encodedPassword shouldBe "FAKE_ENCODED:mySecretPassword123"
    }

    test("should match raw password with encoded password when they are the same") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = "correctPassword"
        val encodedPassword = "FAKE_ENCODED:correctPassword"

        // When
        val matches = userPasswordService.matches(rawPassword, encodedPassword)

        // Then
        matches shouldBe true
    }

    test("should not match raw password with encoded password when they are different") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = "wrongPassword"
        val encodedPassword = "FAKE_ENCODED:correctPassword"

        // When
        val matches = userPasswordService.matches(rawPassword, encodedPassword)

        // Then
        matches shouldBe false
    }

    test("should encode empty password") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = ""

        // When
        val encodedPassword = userPasswordService.encode(rawPassword)

        // Then
        encodedPassword shouldBe "FAKE_ENCODED:"
    }

    test("should encode password with special characters") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = "p@ssw0rd!#$%^&*()"

        // When
        val encodedPassword = userPasswordService.encode(rawPassword)

        // Then
        encodedPassword shouldBe "FAKE_ENCODED:p@ssw0rd!#$%^&*()"
    }

    test("should encode very long password") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = "a".repeat(1000)

        // When
        val encodedPassword = userPasswordService.encode(rawPassword)

        // Then
        encodedPassword shouldBe "FAKE_ENCODED:${"a".repeat(1000)}"
    }

    test("should handle whitespace in password") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = "  password with spaces  "

        // When
        val encodedPassword = userPasswordService.encode(rawPassword)

        // Then
        encodedPassword shouldBe "FAKE_ENCODED:  password with spaces  "
    }

    test("should not match when encoded password is malformed") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = "password"
        val malformedEncodedPassword = "WRONG_PREFIX:password"

        // When
        val matches = userPasswordService.matches(rawPassword, malformedEncodedPassword)

        // Then
        matches shouldBe false
    }
})
