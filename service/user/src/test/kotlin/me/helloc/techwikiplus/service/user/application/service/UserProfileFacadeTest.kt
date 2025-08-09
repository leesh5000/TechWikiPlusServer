package me.helloc.techwikiplus.service.user.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.FakeAuthorizationPort
import me.helloc.techwikiplus.service.user.domain.port.FakeUserRepository
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import java.time.Instant

class GetUserProfileFacadeTest : DescribeSpec({
    lateinit var facade: UserProfileFacade
    lateinit var userRepository: FakeUserRepository
    lateinit var authorizationPort: FakeAuthorizationPort
    lateinit var userReader: UserReader

    beforeEach {
        userRepository = FakeUserRepository()
        authorizationPort = FakeAuthorizationPort()
        userReader = UserReader(userRepository)
        facade = UserProfileFacade(userReader)
    }

    describe("execute") {
        context("사용자가 자신의 프로필을 조회할 때") {
            it("프로필 정보를 반환한다") {
                // Given
                val userId = UserId("test-user-id")
                val user =
                    User(
                        id = userId,
                        email = Email("test@example.com"),
                        encodedPassword = EncodedPassword("encoded"),
                        nickname = Nickname("testuser"),
                        role = UserRole.USER,
                        status = UserStatus.ACTIVE,
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now(),
                    )
                userRepository.save(user)
                authorizationPort.setCurrentUser(userId, UserRole.USER)

                // When
                val result = facade.execute(userId)

                // Then
                result.userId shouldBe userId
                result.email shouldBe "test@example.com"
                result.nickname shouldBe "testuser"
                result.role shouldBe UserRole.USER
                result.status shouldBe UserStatus.ACTIVE
            }
        }

        context("관리자가 다른 사용자의 프로필을 조회할 때") {
            it("프로필 정보를 반환한다") {
                // Given
                val adminUserId = UserId("admin-user-id")
                val targetUserId = UserId("target-user-id")

                val adminUser =
                    User(
                        id = adminUserId,
                        email = Email("admin@example.com"),
                        encodedPassword = EncodedPassword("encoded"),
                        nickname = Nickname("admin"),
                        role = UserRole.ADMIN,
                        status = UserStatus.ACTIVE,
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now(),
                    )

                val targetUser =
                    User(
                        id = targetUserId,
                        email = Email("target@example.com"),
                        encodedPassword = EncodedPassword("encoded"),
                        nickname = Nickname("targetuser"),
                        role = UserRole.USER,
                        status = UserStatus.ACTIVE,
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now(),
                    )

                userRepository.save(adminUser)
                userRepository.save(targetUser)
                authorizationPort.setCurrentUser(adminUserId, UserRole.ADMIN)

                // When
                val result = facade.execute(targetUserId)

                // Then
                result.userId shouldBe targetUserId
                result.email shouldBe "target@example.com"
                result.nickname shouldBe "targetuser"
            }
        }

        context("일반 사용자가 다른 사용자의 프로필을 조회할 때") {
            it("FORBIDDEN 예외를 발생시킨다") {
                // Given
                val currentUserId = UserId("current-user-id")
                val targetUserId = UserId("target-user-id")

                val targetUser =
                    User(
                        id = targetUserId,
                        email = Email("target@example.com"),
                        encodedPassword = EncodedPassword("encoded"),
                        nickname = Nickname("targetuser"),
                        role = UserRole.USER,
                        status = UserStatus.ACTIVE,
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now(),
                    )

                userRepository.save(targetUser)
                authorizationPort.setCurrentUser(currentUserId, UserRole.USER)

                // When & Then
                val exception =
                    shouldThrow<DomainException> {
                        facade.execute(targetUserId)
                    }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }

        context("인증되지 않은 사용자가 프로필을 조회할 때") {
            it("FORBIDDEN 예외를 발생시킨다") {
                // Given
                val targetUserId = UserId("target-user-id")
                authorizationPort.clearCurrentUser()

                // When & Then
                val exception =
                    shouldThrow<DomainException> {
                        facade.execute(targetUserId)
                    }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }

        context("존재하지 않는 사용자의 프로필을 조회할 때") {
            it("USER_NOT_FOUND 예외를 발생시킨다") {
                // Given
                val currentUserId = UserId("current-user-id")
                val nonExistentUserId = UserId("non-existent-user")
                authorizationPort.setCurrentUser(currentUserId, UserRole.ADMIN)

                // When & Then
                val exception =
                    shouldThrow<DomainException> {
                        facade.execute(nonExistentUserId)
                    }
                exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            }
        }
    }
})
