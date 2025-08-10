package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.FakeClockHolder
import me.helloc.techwikiplus.service.user.domain.port.FakeUserRepository
import java.time.Instant

class UserModifierTest : FunSpec({

    lateinit var userModifier: UserModifier
    lateinit var clockHolder: FakeClockHolder
    lateinit var repository: FakeUserRepository

    beforeEach {
        clockHolder = FakeClockHolder(Instant.parse("2025-01-07T10:00:00Z"))
        repository = FakeUserRepository()
        userModifier = UserModifier(clockHolder, repository)
    }

    afterEach {
        repository.clear()
    }

    context("activate 메서드는") {
        test("PENDING 상태의 사용자를 ACTIVE 상태로 변경한다") {
            // given
            val pendingUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(pendingUser)

            // when
            val result = userModifier.activate(pendingUser)

            // then
            result shouldNotBe null
            result.status shouldBe UserStatus.ACTIVE
            result.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
            result.createdAt shouldBe Instant.parse("2025-01-01T00:00:00Z")

            // 저장소에 업데이트되었는지 확인
            val savedUser = repository.findBy(UserId("user-1"))
            savedUser shouldNotBe null
            savedUser?.status shouldBe UserStatus.ACTIVE
            savedUser?.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
        }

        test("이미 ACTIVE 상태인 사용자는 변경하지 않고 그대로 반환한다") {
            // given
            val activeUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(activeUser)

            // when
            val result = userModifier.activate(activeUser)

            // then
            result shouldBe activeUser
            result.modifiedAt shouldBe Instant.parse("2025-01-01T00:00:00Z") // 시간이 변경되지 않음

            // 저장소에서도 동일한지 확인
            val savedUser = repository.findBy(UserId("user-1"))
            savedUser shouldBe activeUser
        }

        test("DORMANT 상태의 사용자를 ACTIVE 상태로 변경한다") {
            // given
            val dormantUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.DORMANT,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(dormantUser)

            // when
            val result = userModifier.activate(dormantUser)

            // then
            result.status shouldBe UserStatus.ACTIVE
            result.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")

            // 저장소에 업데이트되었는지 확인
            val savedUser = repository.findBy(UserId("user-1"))
            savedUser?.status shouldBe UserStatus.ACTIVE
        }

        test("BANNED 상태의 사용자를 ACTIVE 상태로 변경한다") {
            // given
            val bannedUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.BANNED,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(bannedUser)

            // when
            val result = userModifier.activate(bannedUser)

            // then
            result.status shouldBe UserStatus.ACTIVE
            result.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")

            // 저장소에 업데이트되었는지 확인
            val savedUser = repository.findBy(UserId("user-1"))
            savedUser?.status shouldBe UserStatus.ACTIVE
        }

        test("활성화 과정에서 다른 필드는 변경되지 않는다") {
            // given
            val originalUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.PENDING,
                    role = UserRole.ADMIN,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(originalUser)

            // when
            val result = userModifier.activate(originalUser)

            // then
            result.id shouldBe originalUser.id
            result.email shouldBe originalUser.email
            result.encodedPassword shouldBe originalUser.encodedPassword
            result.nickname shouldBe originalUser.nickname
            result.role shouldBe originalUser.role
            result.createdAt shouldBe originalUser.createdAt
            // status와 modifiedAt만 변경됨
            result.status shouldBe UserStatus.ACTIVE
            result.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
        }
    }

    context("setPending 메서드는") {
        test("ACTIVE 상태의 사용자를 PENDING 상태로 변경한다") {
            // given
            val activeUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(activeUser)

            // when
            val result = userModifier.setPending(activeUser)

            // then
            result shouldNotBe null
            result.status shouldBe UserStatus.PENDING
            result.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
            result.createdAt shouldBe Instant.parse("2025-01-01T00:00:00Z")

            // 저장소에 업데이트되었는지 확인
            val savedUser = repository.findBy(UserId("user-1"))
            savedUser shouldNotBe null
            savedUser?.status shouldBe UserStatus.PENDING
            savedUser?.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
        }

        test("이미 PENDING 상태인 사용자는 변경하지 않고 그대로 반환한다") {
            // given
            val pendingUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(pendingUser)

            // when
            val result = userModifier.setPending(pendingUser)

            // then
            result shouldBe pendingUser
            result.modifiedAt shouldBe Instant.parse("2025-01-01T00:00:00Z") // 시간이 변경되지 않음

            // 저장소에서도 동일한지 확인
            val savedUser = repository.findBy(UserId("user-1"))
            savedUser shouldBe pendingUser
        }

        test("DORMANT 상태의 사용자를 PENDING 상태로 변경한다") {
            // given
            val dormantUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.DORMANT,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(dormantUser)

            // when
            val result = userModifier.setPending(dormantUser)

            // then
            result.status shouldBe UserStatus.PENDING
            result.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")

            // 저장소에 업데이트되었는지 확인
            val savedUser = repository.findBy(UserId("user-1"))
            savedUser?.status shouldBe UserStatus.PENDING
        }

        test("PENDING 설정 과정에서 다른 필드는 변경되지 않는다") {
            // given
            val originalUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.ADMIN,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(originalUser)

            // when
            val result = userModifier.setPending(originalUser)

            // then
            result.id shouldBe originalUser.id
            result.email shouldBe originalUser.email
            result.encodedPassword shouldBe originalUser.encodedPassword
            result.nickname shouldBe originalUser.nickname
            result.role shouldBe originalUser.role
            result.createdAt shouldBe originalUser.createdAt
            // status와 modifiedAt만 변경됨
            result.status shouldBe UserStatus.PENDING
            result.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
        }
    }

    context("시간 경과에 따른 테스트") {
        test("시간이 경과한 후 활성화하면 변경된 시간이 반영된다") {
            // given
            val pendingUser =
                User.create(
                    id = UserId("user-1"),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("testuser"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(pendingUser)

            // 시간 경과 시뮬레이션
            clockHolder.advanceTimeBySeconds(3600) // 1시간 경과

            // when
            val result = userModifier.activate(pendingUser)

            // then
            result.modifiedAt shouldBe Instant.parse("2025-01-07T11:00:00Z")
        }

        test("여러 사용자를 순차적으로 처리할 수 있다") {
            // given
            val user1 =
                User.create(
                    id = UserId("user-1"),
                    email = Email("user1@example.com"),
                    encodedPassword = EncodedPassword("encoded_password1"),
                    nickname = Nickname("user1"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            val user2 =
                User.create(
                    id = UserId("user-2"),
                    email = Email("user2@example.com"),
                    encodedPassword = EncodedPassword("encoded_password2"),
                    nickname = Nickname("user2"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(user1)
            repository.save(user2)

            // when
            val activatedUser1 = userModifier.activate(user1)

            // 시간 경과
            clockHolder.advanceTimeBySeconds(60)

            val pendingUser2 = userModifier.setPending(user2)

            // then
            activatedUser1.status shouldBe UserStatus.ACTIVE
            activatedUser1.modifiedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")

            pendingUser2.status shouldBe UserStatus.PENDING
            pendingUser2.modifiedAt shouldBe Instant.parse("2025-01-07T10:01:00Z")

            // 저장소에서 확인
            repository.findBy(UserId("user-1"))?.status shouldBe UserStatus.ACTIVE
            repository.findBy(UserId("user-2"))?.status shouldBe UserStatus.PENDING
        }
    }

    context("격리성 테스트") {
        test("한 사용자의 상태 변경이 다른 사용자에게 영향을 주지 않는다") {
            // given
            val user1 =
                User.create(
                    id = UserId("user-1"),
                    email = Email("user1@example.com"),
                    encodedPassword = EncodedPassword("encoded_password1"),
                    nickname = Nickname("user1"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            val user2 =
                User.create(
                    id = UserId("user-2"),
                    email = Email("user2@example.com"),
                    encodedPassword = EncodedPassword("encoded_password2"),
                    nickname = Nickname("user2"),
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            repository.save(user1)
            repository.save(user2)

            // when
            userModifier.activate(user1)

            // then
            val savedUser1 = repository.findBy(UserId("user-1"))
            val savedUser2 = repository.findBy(UserId("user-2"))

            savedUser1?.status shouldBe UserStatus.ACTIVE
            savedUser2?.status shouldBe UserStatus.PENDING // 변경되지 않음
            savedUser2?.modifiedAt shouldBe Instant.parse("2025-01-01T00:00:00Z") // 시간도 변경되지 않음
        }
    }
})
