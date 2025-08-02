package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordEncoder
import java.time.Instant

class UserReaderDomainResultTest : FunSpec({

    test("사용자가 존재할 때 사용자를 반환해야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        val passwordEncoder = FakePasswordEncoder()

        val existingUser =
            User.create(
                id = "123456789",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(passwordEncoder.encode("Password123!")),
                createdAt = Instant.now(),
            )
        userRepository.save(existingUser)

        // When
        val result = userReader.findByEmail(Email("user@example.com"))

        // Then
        result.id shouldBe "123456789"
        result.email.value shouldBe "user@example.com"
    }

    test("사용자가 존재하지 않을 때 UserNotFoundException을 발생시켜야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)

        // When/Then
        val exception =
            shouldThrow<UserNotFoundException> {
                userReader.findByEmail(Email("nonexistent@example.com"))
            }
        exception.message shouldBe "User not found: User with email nonexistent@example.com not found"
    }

    test("올바른 속성을 가진 사용자를 반환해야 한다") {
        // Given
        val userRepository = FakeUserRepository()
        val userReader = UserReader(userRepository)
        val passwordEncoder = FakePasswordEncoder()

        val user =
            User.create(
                id = "123",
                email = Email("user@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword(passwordEncoder.encode("Password123!")),
                createdAt = Instant.now(),
            )
        userRepository.save(user)

        // When
        val result = userReader.findByEmail(Email("user@example.com"))

        // Then
        result.nickname.value shouldBe "testuser"
    }
})
