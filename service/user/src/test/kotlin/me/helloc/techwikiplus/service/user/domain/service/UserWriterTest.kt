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

    context("insert 메서드") {
        test("새로운 사용자를 성공적으로 삽입한다") {
            // Given
            val user =
                User(
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
            val insertedUser = userWriter.insert(user)

            // Then
            insertedUser shouldBe user
            repository.findBy(user.email) shouldBe user
        }

        test("이미 존재하는 이메일로 사용자 삽입 시 UserAlreadyExistsException 발생") {
            // Given
            val existingUser =
                User(
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

            val newUser =
                User(
                    id = "new-user-2",
                    // 동일한 이메일
                    email = Email("existing@example.com"),
                    encodedPassword = EncodedPassword("encodedPassword2"),
                    nickname = Nickname("newUser"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = now,
                    modifiedAt = now,
                )

            // When & Then
            val exception =
                shouldThrow<UserAlreadyExistsException.ForEmail> {
                    userWriter.insert(newUser)
                }
            exception.message shouldBe "User with email existing@example.com already exists"

            // 새로운 사용자는 저장되지 않아야 함
            repository.findBy(newUser.email) shouldBe existingUser
        }

        test("PENDING 상태의 사용자도 이메일 중복 검사를 수행한다") {
            // Given
            val pendingUser =
                User(
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

            val newUser =
                User(
                    id = "new-user-2",
                    // 동일한 이메일
                    email = Email("pending@example.com"),
                    encodedPassword = EncodedPassword("encodedPassword2"),
                    nickname = Nickname("newUser"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = now,
                    modifiedAt = now,
                )

            // When & Then
            shouldThrow<UserAlreadyExistsException.ForEmail> {
                userWriter.insert(newUser)
            }
        }

        test("이미 존재하는 닉네임으로 사용자 삽입 시 UserAlreadyExistsException 발생") {
            // Given
            val existingUser =
                User(
                    id = "existing-user-1",
                    email = Email("user1@example.com"),
                    encodedPassword = EncodedPassword("encodedPassword1"),
                    nickname = Nickname("existingNickname"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = now,
                    modifiedAt = now,
                )
            repository.save(existingUser)

            val newUser =
                User(
                    id = "new-user-2",
                    email = Email("user2@example.com"),
                    encodedPassword = EncodedPassword("encodedPassword2"),
                    // 동일한 닉네임
                    nickname = Nickname("existingNickname"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = now,
                    modifiedAt = now,
                )

            // When & Then
            val exception =
                shouldThrow<UserAlreadyExistsException.ForNickname> {
                    userWriter.insert(newUser)
                }
            exception.message shouldBe "User with nickname existingNickname already exists"

            // 새로운 사용자는 저장되지 않아야 함
            repository.exists(newUser.email) shouldBe false
        }
    }

    context("update 메서드") {
        test("기존 사용자를 성공적으로 업데이트한다") {
            // Given
            val originalUser =
                User(
                    id = "user-1",
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("originalPassword"),
                    nickname = Nickname("originalNickname"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = now.minusSeconds(3600),
                    modifiedAt = now.minusSeconds(3600),
                )
            repository.save(originalUser)

            val updatedUser =
                originalUser.copy(
                    status = UserStatus.ACTIVE,
                    nickname = Nickname("updatedNickname"),
                    modifiedAt = now,
                )

            // When
            val result = userWriter.update(updatedUser)

            // Then
            result shouldBe updatedUser
            val savedUser = repository.findBy(originalUser.email)
            savedUser?.status shouldBe UserStatus.ACTIVE
            savedUser?.nickname?.value shouldBe "updatedNickname"
            savedUser?.modifiedAt shouldBe now
        }

        test("업데이트 시 이메일 중복 검사를 수행하지 않는다") {
            // Given
            val user1 =
                User(
                    id = "user-1",
                    email = Email("user1@example.com"),
                    encodedPassword = EncodedPassword("password1"),
                    nickname = Nickname("user1"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = now,
                    modifiedAt = now,
                )
            repository.save(user1)

            // When
            val updatedUser1 = user1.copy(nickname = Nickname("updatedUser1"))
            val result = userWriter.update(updatedUser1)

            // Then - 중복 검사 없이 업데이트 성공
            result shouldBe updatedUser1
        }

        test("업데이트된 사용자의 모든 속성이 올바르게 유지된다") {
            // Given
            val user =
                User(
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
            val updatedUser = userWriter.update(user)

            // Then
            updatedUser.id shouldBe "complete-user-12345"
            updatedUser.email.value shouldBe "complete@example.com"
            updatedUser.encodedPassword.value shouldBe "verySecurePassword"
            updatedUser.nickname.value shouldBe "completeUser"
            updatedUser.status shouldBe UserStatus.ACTIVE
            updatedUser.role shouldBe UserRole.ADMIN
            updatedUser.createdAt shouldBe now.minusSeconds(3600)
            updatedUser.modifiedAt shouldBe now
        }
    }

    test("다른 이메일의 사용자는 정상적으로 삽입된다") {
        // Given
        val user1 =
            User(
                id = "user-1",
                email = Email("user1@example.com"),
                encodedPassword = EncodedPassword("encodedPassword1"),
                nickname = Nickname("user1"),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                modifiedAt = now,
            )
        val user2 =
            User(
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
        userWriter.insert(user1)
        val insertedUser2 = userWriter.insert(user2)

        // Then
        insertedUser2 shouldBe user2
        repository.findBy(user1.email) shouldBe user1
        repository.findBy(user2.email) shouldBe user2
    }
})
