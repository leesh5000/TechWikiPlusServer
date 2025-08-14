package me.helloc.techwikiplus.service.user.application.facade

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.infrastructure.FakeUserAuthorizationPort
import me.helloc.techwikiplus.service.user.infrastructure.FakeUserRepository
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import me.helloc.techwikiplus.service.user.domain.service.UserAuthorizationService
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import java.time.Instant

class MyProfileFacadeTest : DescribeSpec({
    lateinit var facade: MyProfileFacade
    lateinit var userRepository: FakeUserRepository
    lateinit var authorizationPort: FakeUserAuthorizationPort
    lateinit var userReader: UserReader
    lateinit var authorizationService: UserAuthorizationService

    beforeEach {
        userRepository = FakeUserRepository()
        authorizationPort = FakeUserAuthorizationPort()
        userReader = UserReader(userRepository)
        authorizationService = UserAuthorizationService(authorizationPort)
        facade = MyProfileFacade(userReader, authorizationService)
    }

    describe("execute") {
        context("인증된 사용자가 자신의 프로필을 조회할 때") {
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
                authorizationPort.setCurrentUser(userId)

                // When
                val result = facade.execute()

                // Then
                result.userId shouldBe userId
                result.email shouldBe "test@example.com"
                result.nickname shouldBe "testuser"
                result.role shouldBe UserRole.USER
                result.status shouldBe UserStatus.ACTIVE
            }
        }

        context("인증되지 않은 사용자가 프로필을 조회할 때") {
            it("UNAUTHORIZED 예외를 발생시킨다") {
                // Given
                authorizationPort.clearCurrentUser()

                // When & Then
                val exception =
                    shouldThrow<UserDomainException> {
                        facade.execute()
                    }
                exception.userErrorCode shouldBe UserErrorCode.UNAUTHORIZED
            }
        }

        context("사용자가 존재하지 않을 때") {
            it("USER_NOT_FOUND 예외를 발생시킨다") {
                // Given
                val userId = UserId(9999999L)
                authorizationPort.setCurrentUser(userId)

                // When & Then
                val exception =
                    shouldThrow<UserDomainException> {
                        facade.execute()
                    }
                exception.userErrorCode shouldBe UserErrorCode.USER_NOT_FOUND
            }
        }
    }
})
