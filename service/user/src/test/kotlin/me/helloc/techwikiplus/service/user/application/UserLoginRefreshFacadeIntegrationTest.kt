package me.helloc.techwikiplus.service.user.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.BannedUserException
import me.helloc.techwikiplus.service.user.domain.exception.DormantUserException
import me.helloc.techwikiplus.service.user.domain.exception.ExpiredTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenTypeException
import me.helloc.techwikiplus.service.user.domain.exception.PendingUserException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.service.UserAuthenticator
import me.helloc.techwikiplus.service.user.domain.service.UserReader
import me.helloc.techwikiplus.service.user.domain.service.UserTokenGenerator
import me.helloc.techwikiplus.service.user.domain.service.UserTokenValidator
import me.helloc.techwikiplus.service.user.domain.service.port.TokenValidator
import me.helloc.techwikiplus.service.user.infrastructure.clock.FakeClockHolder
import me.helloc.techwikiplus.service.user.infrastructure.persistence.FakeUserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordCrypter
import me.helloc.techwikiplus.service.user.infrastructure.security.FakeTokenGenerator
import me.helloc.techwikiplus.service.user.infrastructure.token.FakeTokenValidator
import me.helloc.techwikiplus.service.user.interfaces.usecase.UserLoginRefreshUseCase
import java.time.Instant

class UserLoginRefreshFacadeIntegrationTest : FunSpec({

    test("유효한 리프레시 토큰으로 새로운 액세스 토큰을 발급받아야 한다") {
        // given
        val repository = FakeUserRepository()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)
        val tokenValidator = FakeTokenValidator(clockHolder)
        val tokenGenerator = FakeTokenGenerator()
        val crypter = FakePasswordCrypter()

        // 테스트 사용자 생성
        val userId = "test-user-id"
        val user =
            User(
                id = userId,
                email = Email("test@example.com"),
                nickname = Nickname("testuser"),
                encodedPassword = EncodedPassword("{noop}password"),
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
                createdAt = now.minusSeconds(3600),
                modifiedAt = now.minusSeconds(3600),
            )
        repository.save(user)

        // 리프레시 토큰 설정
        val refreshToken = "valid.refresh.token"
        tokenValidator.addValidRefreshToken(refreshToken, userId)

        // 의존성 생성
        val userReader = UserReader(repository)
        val userTokenGenerator = UserTokenGenerator(tokenGenerator, clockHolder)
        val userTokenValidator = UserTokenValidator(tokenValidator)
        val authenticator = UserAuthenticator(crypter)

        // 파사드 생성
        val facade =
            UserLoginRefreshFacade(
                userReader,
                authenticator,
                userTokenGenerator,
                userTokenValidator,
            )

        // when
        val command = UserLoginRefreshUseCase.Command(refreshToken = refreshToken)
        val result = facade.execute(command)

        // then
        result.userId shouldBe userId
        result.accessToken shouldNotBe null
        result.refreshToken shouldNotBe null
        result.accessTokenExpiresAt shouldBe now.plusSeconds(3600) // 1시간
        result.refreshTokenExpiresAt shouldBe now.plusSeconds(2592000) // 30일
    }

    test("잘못된 형식의 리프레시 토큰으로 요청 시 InvalidTokenException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)
        val tokenValidator = FakeTokenValidator(clockHolder)
        val crypter = FakePasswordCrypter()
        val tokenGenerator = FakeTokenGenerator()

        // 의존성 생성
        val userReader = UserReader(repository)
        val userTokenGenerator = UserTokenGenerator(tokenGenerator, clockHolder)
        val userTokenValidator = UserTokenValidator(tokenValidator)
        val authenticator = UserAuthenticator(crypter)

        // 파사드 생성
        val facade =
            UserLoginRefreshFacade(
                userReader,
                authenticator,
                userTokenGenerator,
                userTokenValidator,
            )

        // when/then
        val command = UserLoginRefreshUseCase.Command(refreshToken = "invalid.token.format")

        shouldThrow<InvalidTokenException> {
            facade.execute(command)
        }
    }

    test("만료된 리프레시 토큰으로 요청 시 ExpiredTokenException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)
        val tokenValidator = FakeTokenValidator(clockHolder)
        val crypter = FakePasswordCrypter()

        // 만료된 토큰 설정
        val expiredToken = "expired.refresh.token"
        tokenValidator.addValidToken(
            expiredToken,
            TokenValidator.TokenClaims(
                userId = "test-user-id",
                tokenType = "refresh",
                // 31일 전
                issuedAt = now.minusSeconds(86400 * 31),
                // 1일 전에 만료
                expiresAt = now.minusSeconds(86400),
            ),
        )

        val tokenGenerator = FakeTokenGenerator()

        // 의존성 생성
        val userReader = UserReader(repository)
        val userTokenGenerator = UserTokenGenerator(tokenGenerator, clockHolder)
        val userTokenValidator = UserTokenValidator(tokenValidator)
        val authenticator = UserAuthenticator(crypter)

        // 파사드 생성
        val facade =
            UserLoginRefreshFacade(
                userReader,
                authenticator,
                userTokenGenerator,
                userTokenValidator,
            )

        // when/then
        val command = UserLoginRefreshUseCase.Command(refreshToken = expiredToken)

        shouldThrow<ExpiredTokenException> {
            facade.execute(command)
        }
    }

    test("액세스 토큰을 리프레시 토큰으로 사용 시 InvalidTokenTypeException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)
        val tokenValidator = FakeTokenValidator(clockHolder)
        val crypter = FakePasswordCrypter()

        // 액세스 토큰을 설정 (타입이 access)
        val accessToken = "access.token"
        tokenValidator.addValidToken(
            accessToken,
            TokenValidator.TokenClaims(
                userId = "test-user-id",
                tokenType = "access",
                issuedAt = now.minusSeconds(3600),
                expiresAt = now.plusSeconds(3600),
            ),
        )
        val tokenGenerator = FakeTokenGenerator()

        // 의존성 생성
        val userReader = UserReader(repository)
        val userTokenGenerator = UserTokenGenerator(tokenGenerator, clockHolder)
        val userTokenValidator = UserTokenValidator(tokenValidator)
        val authenticator = UserAuthenticator(crypter)

        // 파사드 생성
        val facade =
            UserLoginRefreshFacade(
                userReader,
                authenticator,
                userTokenGenerator,
                userTokenValidator,
            )

        // when/then
        val command = UserLoginRefreshUseCase.Command(refreshToken = accessToken)

        shouldThrow<InvalidTokenTypeException> {
            facade.execute(command)
        }
    }

    test("존재하지 않는 사용자의 리프레시 토큰으로 요청 시 UserNotFoundException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)
        val tokenValidator = FakeTokenValidator(clockHolder)
        val crypter = FakePasswordCrypter()

        // 존재하지 않는 사용자의 토큰 설정
        val refreshToken = "valid.refresh.token"
        tokenValidator.addValidRefreshToken(refreshToken, "non-existent-user-id")
        val tokenGenerator = FakeTokenGenerator()

        // 의존성 생성
        val userReader = UserReader(repository)
        val userTokenGenerator = UserTokenGenerator(tokenGenerator, clockHolder)
        val userTokenValidator = UserTokenValidator(tokenValidator)
        val authenticator = UserAuthenticator(crypter)

        // 파사드 생성
        val facade =
            UserLoginRefreshFacade(
                userReader,
                authenticator,
                userTokenGenerator,
                userTokenValidator,
            )

        // when/then
        val command = UserLoginRefreshUseCase.Command(refreshToken = refreshToken)

        shouldThrow<UserNotFoundException> {
            facade.execute(command)
        }
    }

    test("PENDING 사용자의 리프레시 토큰으로 요청 시 PendingUserException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)
        val tokenValidator = FakeTokenValidator(clockHolder)
        val crypter = FakePasswordCrypter()

        // PENDING 사용자 생성
        val userId = "pending-user-id"
        val user =
            User(
                id = userId,
                email = Email("pending@example.com"),
                nickname = Nickname("pendinguser"),
                encodedPassword = EncodedPassword("{noop}password"),
                role = UserRole.USER,
                status = UserStatus.PENDING,
                createdAt = now.minusSeconds(3600),
                modifiedAt = now.minusSeconds(3600),
            )
        repository.save(user)

        // 리프레시 토큰 설정
        val refreshToken = "pending.refresh.token"
        tokenValidator.addValidRefreshToken(refreshToken, userId)
        val tokenGenerator = FakeTokenGenerator()

        // 의존성 생성
        val userReader = UserReader(repository)
        val userTokenGenerator = UserTokenGenerator(tokenGenerator, clockHolder)
        val userTokenValidator = UserTokenValidator(tokenValidator)
        val authenticator = UserAuthenticator(crypter)

        // 파사드 생성
        val facade =
            UserLoginRefreshFacade(
                userReader,
                authenticator,
                userTokenGenerator,
                userTokenValidator,
            )

        // when/then
        val command = UserLoginRefreshUseCase.Command(refreshToken = refreshToken)

        shouldThrow<PendingUserException> {
            facade.execute(command)
        }
    }

    test("DORMANT 사용자의 리프레시 토큰으로 요청 시 DormantUserException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)
        val tokenValidator = FakeTokenValidator(clockHolder)
        val crypter = FakePasswordCrypter()

        // DORMANT 사용자 생성
        val userId = "dormant-user-id"
        val user =
            User(
                id = userId,
                email = Email("dormant@example.com"),
                nickname = Nickname("dormantuser"),
                encodedPassword = EncodedPassword("{noop}password"),
                role = UserRole.USER,
                status = UserStatus.DORMANT,
                createdAt = now.minusSeconds(3600),
                modifiedAt = now.minusSeconds(3600),
            )
        repository.save(user)

        // 리프레시 토큰 설정
        val refreshToken = "dormant.refresh.token"
        tokenValidator.addValidRefreshToken(refreshToken, userId)

        val tokenGenerator = FakeTokenGenerator()

        // 의존성 생성
        val userReader = UserReader(repository)
        val userTokenGenerator = UserTokenGenerator(tokenGenerator, clockHolder)
        val userTokenValidator = UserTokenValidator(tokenValidator)
        val authenticator = UserAuthenticator(crypter)

        // 파사드 생성
        val facade =
            UserLoginRefreshFacade(
                userReader,
                authenticator,
                userTokenGenerator,
                userTokenValidator,
            )

        // when/then
        val command = UserLoginRefreshUseCase.Command(refreshToken = refreshToken)

        shouldThrow<DormantUserException> {
            facade.execute(command)
        }
    }

    test("BANNED 사용자의 리프레시 토큰으로 요청 시 BannedUserException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)
        val tokenValidator = FakeTokenValidator(clockHolder)
        val crypter = FakePasswordCrypter()

        // BANNED 사용자 생성
        val userId = "banned-user-id"
        val user =
            User(
                id = userId,
                email = Email("banned@example.com"),
                nickname = Nickname("banneduser"),
                encodedPassword = EncodedPassword("{noop}password"),
                role = UserRole.USER,
                status = UserStatus.BANNED,
                createdAt = now.minusSeconds(3600),
                modifiedAt = now.minusSeconds(3600),
            )
        repository.save(user)

        // 리프레시 토큰 설정
        val refreshToken = "banned.refresh.token"
        tokenValidator.addValidRefreshToken(refreshToken, userId)
        val tokenGenerator = FakeTokenGenerator()

        // 의존성 생성
        val userReader = UserReader(repository)
        val userTokenGenerator = UserTokenGenerator(tokenGenerator, clockHolder)
        val userTokenValidator = UserTokenValidator(tokenValidator)
        val authenticator = UserAuthenticator(crypter)

        // 파사드 생성
        val facade =
            UserLoginRefreshFacade(
                userReader,
                authenticator,
                userTokenGenerator,
                userTokenValidator,
            )

        // when/then
        val command = UserLoginRefreshUseCase.Command(refreshToken = refreshToken)

        shouldThrow<BannedUserException> {
            facade.execute(command)
        }
    }

    test("DELETED 사용자의 리프레시 토큰으로 요청 시 UserNotFoundException을 던져야 한다") {
        // given
        val repository = FakeUserRepository()
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val clockHolder = FakeClockHolder(now = now)
        val tokenValidator = FakeTokenValidator(clockHolder)
        val crypter = FakePasswordCrypter()

        // DELETED 사용자 생성
        val userId = "deleted-user-id"
        val user =
            User(
                id = userId,
                email = Email("deleted@example.com"),
                nickname = Nickname("deleteduser"),
                encodedPassword = EncodedPassword("{noop}password"),
                role = UserRole.USER,
                status = UserStatus.DELETED,
                createdAt = now.minusSeconds(3600),
                modifiedAt = now.minusSeconds(3600),
            )
        repository.save(user)

        // 리프레시 토큰 설정
        val refreshToken = "deleted.refresh.token"
        tokenValidator.addValidRefreshToken(refreshToken, userId)
        val tokenGenerator = FakeTokenGenerator()

        // 의존성 생성
        val userReader = UserReader(repository)
        val userTokenGenerator = UserTokenGenerator(tokenGenerator, clockHolder)
        val userTokenValidator = UserTokenValidator(tokenValidator)
        val authenticator = UserAuthenticator(crypter)

        // 파사드 생성
        val facade =
            UserLoginRefreshFacade(
                userReader,
                authenticator,
                userTokenGenerator,
                userTokenValidator,
            )

        // when/then
        val command = UserLoginRefreshUseCase.Command(refreshToken = refreshToken)

        shouldThrow<UserNotFoundException> {
            facade.execute(command)
        }
    }
})
