package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.result.DomainResult
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.model.value.Nickname
import me.helloc.techwikiplus.domain.model.value.Password
import me.helloc.techwikiplus.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.infrastructure.security.FakePasswordEncoder
import java.time.Instant

class UserReaderDomainResultTest : FunSpec({

    test("should return Success with user when user exists") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        val passwordEncoder = FakePasswordEncoder()

        val existingUser =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password123", passwordEncoder),
                createdAt = Instant.now(),
            )
        userRepository.save(existingUser)

        // When
        val result = userReader.findByEmail(Email("user@example.com"))

        // Then
        result.shouldBeInstanceOf<DomainResult.Success<User>>()
        result.data.id shouldBe "123456789"
        result.data.email.value shouldBe "user@example.com"
    }

    test("should return NotFound when user does not exist") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)

        // When
        val result = userReader.findByEmail(Email("nonexistent@example.com"))

        // Then
        result.shouldBeInstanceOf<DomainResult.Failure.NotFound>()
        result.entity shouldBe "User"
        result.id shouldBe "nonexistent@example.com"
    }

    test("should use map function to transform success result") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        val passwordEncoder = FakePasswordEncoder()

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password", passwordEncoder),
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When
        val result =
            userReader.findByEmail(Email("user@example.com"))
                .map { it.nickname.value }

        // Then
        result.shouldBeInstanceOf<DomainResult.Success<String>>()
        result.data shouldBe "testuser"
    }

    test("should propagate failure when mapping") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)

        // When
        val result =
            userReader.findByEmail(Email("nonexistent@example.com"))
                .map { it.nickname.value }

        // Then
        result.shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }

    test("should execute onSuccess callback only for successful results") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        val passwordEncoder = FakePasswordEncoder()
        var callbackExecuted = false

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                password = Password.fromRawPassword("password", passwordEncoder),
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When
        userReader.findByEmail(Email("user@example.com"))
            .onSuccess { callbackExecuted = true }

        // Then
        callbackExecuted shouldBe true
    }

    test("should execute onFailure callback only for failed results") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        var failureReason: DomainResult.Failure? = null

        // When
        userReader.findByEmail(Email("nonexistent@example.com"))
            .onFailure { failureReason = it }

        // Then
        failureReason.shouldBeInstanceOf<DomainResult.Failure.NotFound>()
    }
})
