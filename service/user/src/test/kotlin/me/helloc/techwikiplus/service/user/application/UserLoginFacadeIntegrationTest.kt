package me.helloc.techwikiplus.service.user.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.domain.service.TokenService
import me.helloc.techwikiplus.service.user.domain.service.UserPasswordService
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.infrastructure.clock.FakeClockHolder
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordEncoder
import me.helloc.techwikiplus.service.user.infrastructure.security.FakeTokenGenerator
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserLoginUseCase
import java.time.Instant

class UserLoginFacadeIntegrationTest : FunSpec({

    test("유효한 자격증명으로 로그인 시 토큰과 사용자 정보를 반환해야 한다") {
        // given
        val repository = FakeUserRepository()
        val passwordEncoder = FakePasswordEncoder()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)

        // 테스트 사용자 생성
        val user =
            User(
                id = "1",
                email = Email("test@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = passwordEncoder.encode(RawPassword("Password123!")),
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
                createdAt = now.minusSeconds(3600),
                modifiedAt = now.minusSeconds(3600),
            )
        repository.save(user)

        // 의존성 생성
        val userPasswordService = UserPasswordService(passwordEncoder)
        val userReader = UserReader(repository)
        val tokenService = TokenService(FakeTokenGenerator(), clockHolder)

        // 파사드 생성
        val facade = UserLoginFacade(userReader, userPasswordService, tokenService)

        // when
        val command =
            UserLoginUseCase.Command(
                email = "test@example.com",
                password = "Password123!",
            )
        val result = facade.execute(command)

        // then
        result.userId shouldBe "1"
        result.accessToken shouldNotBe null
        result.refreshToken shouldNotBe null
        result.accessTokenExpiresAt shouldBe now.plusSeconds(3600) // 1시간
        result.refreshTokenExpiresAt shouldBe now.plusSeconds(2592000) // 30일
    }

    test("잘못된 비밀번호로 로그인 시 InvalidCredentialsException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val passwordEncoder = FakePasswordEncoder()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)

        // 테스트 사용자 생성
        val user =
            User(
                id = "1",
                email = Email("test@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = passwordEncoder.encode(RawPassword("Password123!")),
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
                createdAt = now.minusSeconds(3600),
                modifiedAt = now.minusSeconds(3600),
            )
        repository.save(user)

        // 의존성 생성
        val userPasswordService = UserPasswordService(passwordEncoder)
        val userReader = UserReader(repository)
        val tokenService = TokenService(FakeTokenGenerator(), clockHolder)

        // 파사드 생성
        val facade = UserLoginFacade(userReader, userPasswordService, tokenService)

        // when/then
        val command =
            UserLoginUseCase.Command(
                email = "test@example.com",
                password = "WrongPassword123!",
            )

        shouldThrow<InvalidCredentialsException> {
            facade.execute(command)
        }
    }

    test("존재하지 않는 이메일로 로그인 시 UserNotFoundException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val passwordEncoder = FakePasswordEncoder()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)

        // 의존성 생성
        val userPasswordService = UserPasswordService(passwordEncoder)
        val userReader = UserReader(repository)
        val tokenService = TokenService(FakeTokenGenerator(), clockHolder)

        // 파사드 생성
        val facade = UserLoginFacade(userReader, userPasswordService, tokenService)

        // when/then
        val command =
            UserLoginUseCase.Command(
                email = "nonexistent@example.com",
                password = "Password123!",
            )

        shouldThrow<UserNotFoundException> {
            facade.execute(command)
        }
    }

    test("비활성 사용자로 로그인 시 UserNotFoundException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val passwordEncoder = FakePasswordEncoder()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)

        val inactiveStatuses =
            listOf(
                UserStatus.DORMANT to "User account is dormant",
                UserStatus.BANNED to "User account is banned",
                UserStatus.PENDING to "User account is pending activation",
                UserStatus.DELETED to "User account is deleted",
            )

        inactiveStatuses.forEach { (status, expectedMessage) ->
            // 테스트 사용자 생성
            val user =
                User(
                    id = "1",
                    email = Email("test@example.com"),
                    nickname = Nickname("testuser"),
                    encodedPassword = passwordEncoder.encode(RawPassword("Password123!")),
                    role = UserRole.USER,
                    status = status,
                    createdAt = now.minusSeconds(3600),
                    modifiedAt = now.minusSeconds(3600),
                )
            repository.save(user)

            // 의존성 생성
            val userPasswordService = UserPasswordService(passwordEncoder)
            val userReader = UserReader(repository)
            val tokenService = TokenService(FakeTokenGenerator(), clockHolder)

            // 파사드 생성
            val facade = UserLoginFacade(userReader, userPasswordService, tokenService)

            // when/then
            val command =
                UserLoginUseCase.Command(
                    email = "test@example.com",
                    password = "Password123!",
                )

            shouldThrow<UserNotFoundException> {
                facade.execute(command)
            }

            // Clean up for next iteration
            repository.clear()
        }
    }
})
