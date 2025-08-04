package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import java.time.Instant

class UserReaderUnitTest : FunSpec({
    lateinit var userReader: UserReader
    lateinit var fakeRepository: FakeUserRepository

    beforeEach {
        fakeRepository = FakeUserRepository()
        userReader = UserReader(fakeRepository)
    }

    afterEach {
        fakeRepository.clear()
    }

    context("getPendingUserBy 메서드") {
        test("대기 중인 사용자가 존재할 때 해당 사용자를 반환한다") {
            // Given
            val email = Email("test@example.com")
            val now = Instant.now()
            val pendingUser =
                User.create(
                    id = "user-1",
                    email = email,
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded-password"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = now,
                )
            fakeRepository.save(pendingUser)

            // When
            val result = userReader.getPendingUserBy(email)

            // Then
            result shouldBe pendingUser
            result.status shouldBe UserStatus.PENDING
        }

        test("대기 중인 사용자가 존재하지 않을 때 UserNotFoundException을 던진다") {
            // Given
            val email = Email("nonexistent@example.com")

            // When & Then
            val exception =
                shouldThrow<UserNotFoundException> {
                    userReader.getPendingUserBy(email)
                }
            exception.message shouldBe
                "User not found: User with email nonexistent@example.com and status PENDING not found"
        }

        test("사용자는 존재하지만 대기 상태가 아닐 때 UserNotFoundException을 던진다") {
            // Given
            val email = Email("active@example.com")
            val now = Instant.now()
            val activeUser =
                User.create(
                    id = "user-2",
                    email = email,
                    nickname = Nickname("activeuser"),
                    encodedPassword = EncodedPassword("encoded-password"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = now,
                )
            fakeRepository.save(activeUser)

            // When & Then
            val exception =
                shouldThrow<UserNotFoundException> {
                    userReader.getPendingUserBy(email)
                }
            exception.message shouldBe
                "User not found: User with email active@example.com and status PENDING not found"
        }
    }
})
