package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.result.DomainResult
import me.helloc.techwikiplus.domain.model.type.UserStatus
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.model.value.Nickname
import me.helloc.techwikiplus.domain.model.value.Password
import me.helloc.techwikiplus.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.infrastructure.security.FakePasswordEncoder
import java.time.Instant

class UserAuthenticationServiceTest : FunSpec({

    test("should return Success when credentials are valid and user is active") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticationService(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password("FAKE_ENCODED:password123"),
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When
        val result = authService.authenticate(Email("user@example.com"), "password123")

        // Then
        result.shouldBeInstanceOf<DomainResult.Success<User>>()
        result.data.id shouldBe "123"
    }

    test("should return NotFound when user does not exist") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticationService(userRepository, passwordService)

        // When
        val result = authService.authenticate(Email("nonexistent@example.com"), "password")

        // Then
        result.shouldBeInstanceOf<DomainResult.Failure.NotFound>()
        result.entity shouldBe "User"
        result.id shouldBe "nonexistent@example.com"
    }

    test("should return Unauthorized when password is incorrect") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticationService(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password("FAKE_ENCODED:correctPassword"),
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When
        val result = authService.authenticate(Email("user@example.com"), "wrongPassword")

        // Then
        result.shouldBeInstanceOf<DomainResult.Failure.Unauthorized>()
        result.reason shouldBe "Invalid credentials"
    }

    test("should return BusinessRuleViolation when user is not active") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticationService(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password("FAKE_ENCODED:password123"),
                status = UserStatus.DORMANT,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When
        val result = authService.authenticate(Email("user@example.com"), "password123")

        // Then
        result.shouldBeInstanceOf<DomainResult.Failure.BusinessRuleViolation>()
        result.rule shouldBe "User account is not active"
    }

    test("should return BusinessRuleViolation when user is banned") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticationService(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password("FAKE_ENCODED:password123"),
                status = UserStatus.BANNED,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When
        val result = authService.authenticate(Email("user@example.com"), "password123")

        // Then
        result.shouldBeInstanceOf<DomainResult.Failure.BusinessRuleViolation>()
        result.rule shouldBe "User account is banned"
    }

    test("should handle repository errors gracefully") {
        // Given
        val userRepository = FakeUserRepository()
        userRepository.simulateError("Database connection failed")
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticationService(userRepository, passwordService)

        // When
        val result = authService.authenticate(Email("user@example.com"), "password")

        // Then
        result.shouldBeInstanceOf<DomainResult.Failure.SystemError>()
        result.message shouldBe "Database connection failed"
    }
})
