package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.UserAlreadyExistsException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import java.time.Instant

class UserWriterTest : FunSpec({

    val repository = FakeUserRepository()
    val userWriter = UserWriter(repository)
    val now = Instant.now()

    beforeEach {
        repository.clear()
    }

    test("새로운 사용자를 성공적으로 저장한다") {
        // Given
        val user = User(
            id = "test-user-1",
            email = Email("test@example.com"),
            encodedPassword = EncodedPassword("encodedPassword"),
            nickname = Nickname("testUser"),
            status = UserStatus.PENDING,
            role = UserRole.USER,
            createdAt = now,
            modifiedAt = now,
        )

        // When
        val savedUser = userWriter.save(user)

        // Then
        savedUser shouldBe user
        repository.findBy(user.email) shouldBe user
    }

    test("이미 존재하는 이메일로 사용자 저장 시 UserAlreadyExistsException 발생") {
        // Given
        val existingUser = User(
            id = "existing-user-1",
            email = Email("existing@example.com"),
            encodedPassword = EncodedPassword("encodedPassword1"),
            nickname = Nickname("existingUser"),
            status = UserStatus.ACTIVE,
            role = UserRole.USER,
            createdAt = now,
            modifiedAt = now,
        )
        repository.save(existingUser)

        val newUser = User(
            id = "new-user-2",
            email = Email("existing@example.com"), // 동일한 이메일
            encodedPassword = EncodedPassword("encodedPassword2"),
            nickname = Nickname("newUser"),
            status = UserStatus.PENDING,
            role = UserRole.USER,
            createdAt = now,
            modifiedAt = now,
        )

        // When & Then
        val exception = shouldThrow<UserAlreadyExistsException> {
            userWriter.save(newUser)
        }
        exception.message shouldBe "User with email existing@example.com already exists"

        // 새로운 사용자는 저장되지 않아야 함
        repository.findBy(newUser.email) shouldBe existingUser
    }

    test("다른 이메일의 사용자는 정상적으로 저장된다") {
        // Given
        val user1 = User(
            id = "user-1",
            email = Email("user1@example.com"),
            encodedPassword = EncodedPassword("encodedPassword1"),
            nickname = Nickname("user1"),
            status = UserStatus.ACTIVE,
            role = UserRole.USER,
            createdAt = now,
            modifiedAt = now,
        )
        val user2 = User(
            id = "user-2",
            email = Email("user2@example.com"),
            encodedPassword = EncodedPassword("encodedPassword2"),
            nickname = Nickname("user2"),
            status = UserStatus.PENDING,
            role = UserRole.USER,
            createdAt = now,
            modifiedAt = now,
        )

        // When
        userWriter.save(user1)
        val savedUser2 = userWriter.save(user2)

        // Then
        savedUser2 shouldBe user2
        repository.findBy(user1.email) shouldBe user1
        repository.findBy(user2.email) shouldBe user2
    }

    test("PENDING 상태의 사용자도 이메일 중복 검사를 수행한다") {
        // Given
        val pendingUser = User(
            id = "pending-user-1",
            email = Email("pending@example.com"),
            encodedPassword = EncodedPassword("encodedPassword1"),
            nickname = Nickname("pendingUser"),
            status = UserStatus.PENDING,
            role = UserRole.USER,
            createdAt = now,
            modifiedAt = now,
        )
        repository.save(pendingUser)

        val newUser = User(
            id = "new-user-2",
            email = Email("pending@example.com"), // 동일한 이메일
            encodedPassword = EncodedPassword("encodedPassword2"),
            nickname = Nickname("newUser"),
            status = UserStatus.PENDING,
            role = UserRole.USER,
            createdAt = now,
            modifiedAt = now,
        )

        // When & Then
        shouldThrow<UserAlreadyExistsException> {
            userWriter.save(newUser)
        }
    }

    test("저장된 사용자의 모든 속성이 올바르게 유지된다") {
        // Given
        val user = User(
            id = "complete-user-12345",
            email = Email("complete@example.com"),
            encodedPassword = EncodedPassword("verySecurePassword"),
            nickname = Nickname("completeUser"),
            status = UserStatus.ACTIVE,
            role = UserRole.ADMIN,
            createdAt = now.minusSeconds(3600),
            modifiedAt = now,
        )

        // When
        val savedUser = userWriter.save(user)

        // Then
        savedUser.id shouldBe "complete-user-12345"
        savedUser.email.value shouldBe "complete@example.com"
        savedUser.encodedPassword.value shouldBe "verySecurePassword"
        savedUser.nickname.value shouldBe "completeUser"
        savedUser.status shouldBe UserStatus.ACTIVE
        savedUser.role shouldBe UserRole.ADMIN
        savedUser.createdAt shouldBe now.minusSeconds(3600)
        savedUser.modifiedAt shouldBe now
    }
})