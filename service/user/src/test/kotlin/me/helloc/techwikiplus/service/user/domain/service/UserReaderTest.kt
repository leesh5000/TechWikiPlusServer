package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.FakeUserRepository
import java.time.Instant

class UserReaderTest : FunSpec({

    lateinit var userReader: UserReader
    lateinit var repository: FakeUserRepository

    beforeEach {
        repository = FakeUserRepository()
        userReader = UserReader(repository)
    }

    afterEach {
        repository.clear()
    }

    context("get(UserId) 메서드는") {
        test("존재하는 사용자 ID로 조회하면 사용자를 반환한다") {
            // given
            val userId = UserId("user-1")
            val user =
                User.create(
                    id = userId,
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(user)

            // when
            val result = userReader.get(userId)

            // then
            result shouldNotBe null
            result shouldBe user
            result.id shouldBe userId
            result.email shouldBe Email("test@example.com")
            result.nickname shouldBe Nickname("testuser")
            result.status shouldBe UserStatus.ACTIVE
        }

        test("존재하지 않는 사용자 ID로 조회하면 USER_NOT_FOUND 예외를 발생시킨다") {
            // given
            val nonExistentUserId = UserId("non-existent-user")

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(nonExistentUserId)
                }
            exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            exception.params shouldBe arrayOf("non-existent-user")
        }

        test("PENDING 상태의 사용자를 조회하면 USER_PENDING 예외를 발생시킨다") {
            // given
            val userId = UserId("pending-user")
            val pendingUser =
                User.create(
                    id = userId,
                    email = Email("pending@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("pendinguser"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(pendingUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(userId)
                }
            exception.errorCode shouldBe ErrorCode.USER_PENDING
        }

        test("DORMANT 상태의 사용자를 조회하면 USER_DORMANT 예외를 발생시킨다") {
            // given
            val userId = UserId("dormant-user")
            val dormantUser =
                User.create(
                    id = userId,
                    email = Email("dormant@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("dormantuser"),
                    status = UserStatus.DORMANT,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(dormantUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(userId)
                }
            exception.errorCode shouldBe ErrorCode.USER_DORMANT
        }

        test("BANNED 상태의 사용자를 조회하면 USER_BANNED 예외를 발생시킨다") {
            // given
            val userId = UserId("banned-user")
            val bannedUser =
                User.create(
                    id = userId,
                    email = Email("banned@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("banneduser"),
                    status = UserStatus.BANNED,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(bannedUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(userId)
                }
            exception.errorCode shouldBe ErrorCode.USER_BANNED
        }

        test("DELETED 상태의 사용자를 조회하면 USER_DELETED 예외를 발생시킨다") {
            // given
            val userId = UserId("deleted-user")
            val deletedUser =
                User.create(
                    id = userId,
                    email = Email("deleted@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("deleteduser"),
                    status = UserStatus.DELETED,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(deletedUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(userId)
                }
            exception.errorCode shouldBe ErrorCode.USER_DELETED
        }

        test("여러 사용자 중에서 정확한 ID로 사용자를 찾는다") {
            // given
            val user1 =
                User.create(
                    id = UserId("user-1"),
                    email = Email("user1@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("user1"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            val user2 =
                User.create(
                    id = UserId("user-2"),
                    email = Email("user2@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("user2"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            val user3 =
                User.create(
                    id = UserId("user-3"),
                    email = Email("user3@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("user3"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(user1)
            repository.save(user2)
            repository.save(user3)

            // when
            val result = userReader.get(UserId("user-2"))

            // then
            result shouldBe user2
            result.id shouldBe UserId("user-2")
            result.email shouldBe Email("user2@example.com")
        }
    }

    context("get(Email) 메서드는") {
        test("ACTIVE 상태의 사용자를 이메일로 조회하면 성공한다") {
            // given
            val email = Email("active@example.com")
            val activeUser =
                User.create(
                    id = UserId("active-user"),
                    email = email,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("activeuser"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(activeUser)

            // when
            val result = userReader.get(email)

            // then
            result shouldNotBe null
            result shouldBe activeUser
            result.email shouldBe email
            result.status shouldBe UserStatus.ACTIVE
        }

        test("존재하지 않는 이메일로 조회하면 USER_NOT_FOUND 예외를 발생시킨다") {
            // given
            val nonExistentEmail = Email("nonexistent@example.com")

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(nonExistentEmail)
                }
            exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            exception.params shouldBe arrayOf("nonexistent@example.com")
        }

        test("PENDING 상태의 사용자만 있으면 USER_PENDING 예외를 발생시킨다") {
            // given
            val email = Email("pending@example.com")
            val pendingUser =
                User.create(
                    id = UserId("pending-user"),
                    email = email,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("pendinguser"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(pendingUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(email)
                }
            exception.errorCode shouldBe ErrorCode.USER_PENDING
        }

        test("DORMANT 상태의 사용자만 있으면 USER_DORMANT 예외를 발생시킨다") {
            // given
            val email = Email("dormant@example.com")
            val dormantUser =
                User.create(
                    id = UserId("dormant-user"),
                    email = email,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("dormantuser"),
                    status = UserStatus.DORMANT,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(dormantUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(email)
                }
            exception.errorCode shouldBe ErrorCode.USER_DORMANT
        }

        test("BANNED 상태의 사용자만 있으면 USER_BANNED 예외를 발생시킨다") {
            // given
            val email = Email("banned@example.com")
            val bannedUser =
                User.create(
                    id = UserId("banned-user"),
                    email = email,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("banneduser"),
                    status = UserStatus.BANNED,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(bannedUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(email)
                }
            exception.errorCode shouldBe ErrorCode.USER_BANNED
        }

        test("DELETED 상태의 사용자만 있으면 USER_DELETED 예외를 발생시킨다") {
            // given
            val email = Email("deleted@example.com")
            val deletedUser =
                User.create(
                    id = UserId("deleted-user"),
                    email = email,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("deleteduser"),
                    status = UserStatus.DELETED,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(deletedUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(email)
                }
            exception.errorCode shouldBe ErrorCode.USER_DELETED
        }

        test("여러 사용자 중 ACTIVE 상태의 사용자만 조회한다") {
            // given
            val email = Email("test@example.com")

            // 먼저 PENDING 사용자 저장
            val pendingUser =
                User.create(
                    id = UserId("pending-user"),
                    email = Email("pending@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("pendinguser"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(pendingUser)

            // ACTIVE 사용자 저장
            val activeUser =
                User.create(
                    id = UserId("active-user"),
                    email = email,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("activeuser"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(activeUser)

            // when
            val result = userReader.get(email)

            // then
            result shouldBe activeUser
            result.status shouldBe UserStatus.ACTIVE
        }

        test("ADMIN 권한의 ACTIVE 사용자도 조회할 수 있다") {
            // given
            val email = Email("admin@example.com")
            val adminUser =
                User.create(
                    id = UserId("admin-user"),
                    email = email,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("adminuser"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.ADMIN,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(adminUser)

            // when
            val result = userReader.get(email)

            // then
            result shouldBe adminUser
            result.status shouldBe UserStatus.ACTIVE
            result.role shouldBe UserRole.ADMIN
        }
    }

    context("getPendingUser(Email) 메서드는") {
        test("이메일과 상태가 일치하는 사용자를 조회하면 성공한다") {
            // given
            val email = Email("pending@example.com")
            val pendingUser =
                User.create(
                    id = UserId("pending-user"),
                    email = email,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("pendinguser"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(pendingUser)

            // when & then
            shouldNotThrow<DomainException> {
                userReader.getPendingUser(email)
            }
        }

        test("이메일은 존재하지만 상태가 일치하지 않으면 NOT_FOUND_PENDING_USER 예외를 발생시킨다") {
            // given
            val email = Email("active@example.com")
            val activeUser =
                User.create(
                    id = UserId("active-user"),
                    email = email,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("activeuser"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(activeUser)

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.getPendingUser(email)
                }
            exception.errorCode shouldBe ErrorCode.NOT_FOUND_PENDING_USER
            exception.params shouldBe arrayOf(email.value)
        }

        test("이메일이 존재하지 않으면 USER_NOT_FOUND 예외를 발생시킨다") {
            // given
            val nonExistentEmail = Email("nonexistent@example.com")

            // when & then
            val exception =
                shouldThrow<DomainException> {
                    userReader.get(nonExistentEmail)
                }
            exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            exception.params shouldBe arrayOf("nonexistent@example.com")
        }
    }
})
