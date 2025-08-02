package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotActiveException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordEncoder
import java.time.Instant

class UserAuthenticationServiceTest : FunSpec({

    test("should return user when credentials are valid and user is active") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:password123"),
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When
        val result = authService.authenticate(Email("user@example.com"), "password123")

        // Then
        result.id shouldBe "123"
    }

    test("should throw UserNotFoundException when user does not exist") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        // When/Then
        val exception =
            shouldThrow<UserNotFoundException> {
                authService.authenticate(Email("nonexistent@example.com"), "password")
            }
        exception.message shouldBe "User not found: nonexistent@example.com"
    }

    test("should throw InvalidCredentialsException when password is incorrect") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:correctPassword"),
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<InvalidCredentialsException> {
                authService.authenticate(Email("user@example.com"), "wrongPassword")
            }
        exception.message shouldBe "Invalid email or password"
    }

    test("should throw UserNotActiveException when user is dormant") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:password123"),
                status = UserStatus.DORMANT,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<UserNotActiveException> {
                authService.authenticate(Email("user@example.com"), "password123")
            }
        exception.message shouldBe "User account is dormant"
    }

    test("should throw UserNotActiveException when user is banned") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:password123"),
                status = UserStatus.BANNED,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<UserNotActiveException> {
                authService.authenticate(Email("user@example.com"), "password123")
            }
        exception.message shouldBe "User account is banned"
    }

    test("should throw UserNotActiveException for pending users") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:password123"),
                status = UserStatus.PENDING,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<UserNotActiveException> {
                authService.authenticate(Email("user@example.com"), "password123")
            }
        exception.message shouldBe "User account is pending activation"
    }

    test("should throw UserNotActiveException for deleted users") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:password123"),
                status = UserStatus.DELETED,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<UserNotActiveException> {
                authService.authenticate(Email("user@example.com"), "password123")
            }
        exception.message shouldBe "User account is deleted"
    }
})
