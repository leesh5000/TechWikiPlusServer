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
import me.helloc.techwikiplus.service.user.domain.service.UserAuthorizationService
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import java.time.Instant

class MyProfileFacadeTest : DescribeSpec({
    lateinit var facade: MyProfileFacade
    lateinit var userRepository: FakeUserRepository
    lateinit var authorizationPort: FakeAuthorizationPort
    lateinit var userReader: UserReader
    lateinit var authorizationService: UserAuthorizationService

    beforeEach {
        userRepository = FakeUserRepository()
        authorizationPort = FakeAuthorizationPort()
        userReader = UserReader(userRepository)
        authorizationService = UserAuthorizationService(authorizationPort)
        facade = MyProfileFacade(userReader, authorizationService)
    }

    describe("execute") {
        context("인증된 사용자가 자신의 프로필을 조회할 때") {
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
                    shouldThrow<DomainException> {
                        facade.execute()
                    }
                exception.errorCode shouldBe ErrorCode.UNAUTHORIZED
            }
        }

        context("사용자가 존재하지 않을 때") {
            it("USER_NOT_FOUND 예외를 발생시킨다") {
                // Given
                val userId = UserId("non-existent-user")
                authorizationPort.setCurrentUser(userId)

                // When & Then
                val exception =
                    shouldThrow<DomainException> {
                        facade.execute()
                    }
                exception.errorCode shouldBe ErrorCode.USER_NOT_FOUND
            }
        }
    }
})
