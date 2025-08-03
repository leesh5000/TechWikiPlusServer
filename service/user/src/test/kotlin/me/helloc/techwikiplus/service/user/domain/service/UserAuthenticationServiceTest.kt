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
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordEncoder
import java.time.Instant

class UserAuthenticationServiceTest : FunSpec({

    test("자격 증명이 유효하고 사용자가 활성 상태일 때 사용자를 반환해야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:Password123!"),
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When
        val result = authService.authenticate(Email("user@example.com"), RawPassword("Password123!"))

        // Then
        result.id shouldBe "123"
    }

    test("사용자가 존재하지 않을 때 UserNotFoundException을 발생시켜야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        // When/Then
        val exception =
            shouldThrow<UserNotFoundException> {
                authService.authenticate(Email("nonexistent@example.com"), RawPassword("Password123!"))
            }
        exception.message shouldBe "User not found: nonexistent@example.com"
    }

    test("패스워드가 올바르지 않을 때 InvalidCredentialsException을 발생시켜야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:CorrectPassword123!"),
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<InvalidCredentialsException> {
                authService.authenticate(Email("user@example.com"), RawPassword("WrongPassword123!"))
            }
        exception.message shouldBe "Invalid email or password"
    }

    test("사용자가 휴면 상태일 때 UserNotActiveException을 발생시켜야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:Password123!"),
                status = UserStatus.DORMANT,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<UserNotActiveException> {
                authService.authenticate(Email("user@example.com"), RawPassword("Password123!"))
            }
        exception.message shouldBe "User account is dormant"
    }

    test("사용자가 차단 상태일 때 UserNotActiveException을 발생시켜야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:Password123!"),
                status = UserStatus.BANNED,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<UserNotActiveException> {
                authService.authenticate(Email("user@example.com"), RawPassword("Password123!"))
            }
        exception.message shouldBe "User account is banned"
    }

    test("대기 상태 사용자에 대해 UserNotActiveException을 발생시켜야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:Password123!"),
                status = UserStatus.PENDING,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<UserNotActiveException> {
                authService.authenticate(Email("user@example.com"), RawPassword("Password123!"))
            }
        exception.message shouldBe "User account is pending activation"
    }

    test("삭제된 사용자에 대해 UserNotActiveException을 발생시켜야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val passwordService = UserPasswordService(FakePasswordEncoder())
        val authService = UserAuthenticator(userRepository, passwordService)

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("FAKE_ENCODED:Password123!"),
                status = UserStatus.DELETED,
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When/Then
        val exception =
            shouldThrow<UserNotActiveException> {
                authService.authenticate(Email("user@example.com"), RawPassword("Password123!"))
            }
        exception.message shouldBe "User account is deleted"
    }
})
