package me.helloc.techwikiplus.service.user.application.facade

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.common.infrastructure.FakeUserRepository
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import java.time.Instant

class UserProfileFacadeTest : DescribeSpec({
    lateinit var facade: UserProfileFacade
    lateinit var userRepository: FakeUserRepository
    lateinit var userReader: UserReader

    beforeEach {
        userRepository = FakeUserRepository()
        userReader = UserReader(userRepository)
        facade = UserProfileFacade(userReader)
    }

    describe("execute") {
        context("존재하는 사용자의 프로필을 조회할 때") {
            it("프로필 정보를 반환한다") {
                // Given
                val userId = UserId(5000001L)
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

        context("관리자 사용자의 프로필을 조회할 때") {
            it("프로필 정보를 반환한다") {
                // Given
                val adminUserId = UserId(6000001L)

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

                userRepository.save(adminUser)

                // When
                val result = facade.execute(adminUserId)

                // Then
                result.userId shouldBe adminUserId
                result.email shouldBe "admin@example.com"
                result.nickname shouldBe "admin"
                result.role shouldBe UserRole.ADMIN
                result.status shouldBe UserStatus.ACTIVE
            }
        }

        context("존재하지 않는 사용자의 프로필을 조회할 때") {
            it("USER_NOT_FOUND 예외를 발생시킨다") {
                // Given
                val nonExistentUserId = UserId(9999999L)

                // When & Then
                val exception =
                    shouldThrow<UserDomainException> {
                        facade.execute(nonExistentUserId)
                    }
                exception.userErrorCode shouldBe UserErrorCode.USER_NOT_FOUND
            }
        }
    }
})
