package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.common.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.service.common.infrastructure.FakeIdGenerator
import me.helloc.techwikiplus.service.common.infrastructure.FakePasswordEncryptor
import me.helloc.techwikiplus.service.common.infrastructure.FakeUserRepository
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import java.time.Instant

class UserRegisterTest : FunSpec({

    lateinit var userRegister: UserRegister
    lateinit var clockHolder: FakeClockHolder
    lateinit var idGenerator: FakeIdGenerator
    lateinit var repository: FakeUserRepository
    lateinit var passwordEncryptor: FakePasswordEncryptor

    beforeEach {
        clockHolder = FakeClockHolder(Instant.parse("2025-01-07T10:00:00Z"))
        idGenerator = FakeIdGenerator()
        repository = FakeUserRepository()
        passwordEncryptor = FakePasswordEncryptor()
        userRegister = UserRegister(clockHolder, idGenerator, repository, passwordEncryptor)
    }

    afterEach {
        repository.clear()
        idGenerator.reset()
    }

    context("insert 메서드는") {
        test("유효한 정보로 사용자를 성공적으로 등록한다") {
            // given
            val email = Email("test@example.com")
            val nickname = Nickname("testuser")
            val password = RawPassword("Password123!")
            val passwordConfirm = RawPassword("Password123!")

            // when
            val result = userRegister.insert(email, nickname, password, passwordConfirm)

            // then
            result shouldNotBe null
            result.id shouldBe UserId(1000000L)
            result.email shouldBe email
            result.nickname shouldBe nickname
            result.encodedPassword shouldBe EncodedPassword("encoded_Password123!")
            result.status shouldBe UserStatus.ACTIVE
            result.createdAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
            result.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")

            // 저장소에 저장되었는지 확인
            val savedUser = repository.findBy(email)
            savedUser shouldNotBe null
            savedUser?.id shouldBe result.id
        }

        test("비밀번호와 비밀번호 확인이 일치하지 않으면 PASSWORD_MISMATCH 예외를 발생시킨다") {
            // given
            val email = Email("test@example.com")
            val nickname = Nickname("testuser")
            val password = RawPassword("Password123!")
            val passwordConfirm = RawPassword("DifferentPassword123!")

            // when & then
            val exception =
                shouldThrow<UserDomainException> {
                    userRegister.insert(email, nickname, password, passwordConfirm)
                }
            exception.userErrorCode shouldBe UserErrorCode.PASSWORD_MISMATCH

            // 저장소에 저장되지 않았는지 확인
            repository.getAll().size shouldBe 0
        }

        test("이미 존재하는 이메일로 등록하면 DUPLICATE_EMAIL 예외를 발생시킨다") {
            // given
            val existingEmail = Email("existing@example.com")
            val existingUser =
                User.create(
                    id = UserId(2000001L),
                    email = existingEmail,
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("existinguser"),
                    status = UserStatus.ACTIVE,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(existingUser)

            val newNickname = Nickname("newuser")
            val password = RawPassword("Password123!")
            val passwordConfirm = RawPassword("Password123!")

            // when & then
            val exception =
                shouldThrow<UserDomainException> {
                    userRegister.insert(existingEmail, newNickname, password, passwordConfirm)
                }
            exception.userErrorCode shouldBe UserErrorCode.DUPLICATE_EMAIL
            exception.params shouldBe arrayOf(existingEmail.value)

            // 새로운 사용자가 저장되지 않았는지 확인
            repository.getAll().size shouldBe 1
        }

        test("이미 존재하는 닉네임으로 등록하면 DUPLICATE_NICKNAME 예외를 발생시킨다") {
            // given
            val existingNickname = Nickname("existinguser")
            val existingUser =
                User.create(
                    id = UserId(2000001L),
                    email = Email("existing@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = existingNickname,
                    status = UserStatus.ACTIVE,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(existingUser)

            val newEmail = Email("new@example.com")
            val password = RawPassword("Password123!")
            val passwordConfirm = RawPassword("Password123!")

            // when & then
            val exception =
                shouldThrow<UserDomainException> {
                    userRegister.insert(newEmail, existingNickname, password, passwordConfirm)
                }
            exception.userErrorCode shouldBe UserErrorCode.DUPLICATE_NICKNAME
            exception.params shouldBe arrayOf(existingNickname.value)

            // 새로운 사용자가 저장되지 않았는지 확인
            repository.getAll().size shouldBe 1
        }

        test("여러 사용자를 순차적으로 등록할 수 있다") {
            // given
            val user1Email = Email("user1@example.com")
            val user1Nickname = Nickname("user1")
            val user1Password = RawPassword("Password123!")

            val user2Email = Email("user2@example.com")
            val user2Nickname = Nickname("user2")
            val user2Password = RawPassword("Password456!")

            // when
            val user1 = userRegister.insert(user1Email, user1Nickname, user1Password, user1Password)

            // 시간 경과 시뮬레이션
            clockHolder.advanceTimeBySeconds(60)

            val user2 = userRegister.insert(user2Email, user2Nickname, user2Password, user2Password)

            // then
            user1.id shouldBe UserId(1000000L)
            user2.id shouldBe UserId(1000001L)

            repository.getAll().size shouldBe 2
            repository.findBy(user1Email) shouldBe user1
            repository.findBy(user2Email) shouldBe user2
        }
    }

    context("update 메서드는") {
        test("사용자 정보를 성공적으로 업데이트한다") {
            // given
            val originalUser =
                User.create(
                    id = UserId(1000001L),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("originalname"),
                    status = UserStatus.ACTIVE,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(originalUser)

            // 닉네임 변경
            val updatedUser =
                originalUser.copy(
                    nickname = Nickname("updatedname"),
                    modifiedAt = clockHolder.now(),
                )

            // when
            val result = userRegister.update(updatedUser)

            // then
            result shouldBe updatedUser
            val savedUser = repository.findBy(UserId(1000001L))
            savedUser shouldNotBe null
            savedUser?.nickname shouldBe Nickname("updatedname")
            savedUser?.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
        }

        test("존재하지 않는 사용자도 저장할 수 있다") {
            // given
            val newUser =
                User.create(
                    id = UserId(3000001L),
                    email = Email("new@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("newuser"),
                    status = UserStatus.ACTIVE,
                    createdAt = clockHolder.now(),
                    modifiedAt = clockHolder.now(),
                )

            // when
            val result = userRegister.update(newUser)

            // then
            result shouldBe newUser
            repository.findBy(UserId(3000001L)) shouldBe newUser
            repository.getAll().size shouldBe 1
        }

        test("사용자 상태를 변경할 수 있다") {
            // given
            val activeUser =
                User.create(
                    id = UserId(1000001L),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.ACTIVE,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(activeUser)

            val dormantUser =
                activeUser.copy(
                    status = UserStatus.DORMANT,
                    modifiedAt = clockHolder.now(),
                )

            // when
            val result = userRegister.update(dormantUser)

            // then
            result.status shouldBe UserStatus.DORMANT
            val savedUser = repository.findBy(UserId(1000001L))
            savedUser?.status shouldBe UserStatus.DORMANT
        }
    }
})
