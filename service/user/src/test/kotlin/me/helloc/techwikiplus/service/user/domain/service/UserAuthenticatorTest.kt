package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.common.infrastructure.FakePasswordEncryptor
import me.helloc.techwikiplus.service.common.infrastructure.FakeTokenManager
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.RawPassword
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import java.time.Instant

class UserAuthenticatorTest : FunSpec({

    lateinit var userAuthenticator: UserAuthenticator
    lateinit var passwordEncryptor: FakePasswordEncryptor
    lateinit var tokenManager: FakeTokenManager

    beforeEach {
        passwordEncryptor = FakePasswordEncryptor()
        tokenManager = FakeTokenManager()
        userAuthenticator = UserAuthenticator(passwordEncryptor, tokenManager)
    }

    afterEach {
        tokenManager.reset()
    }

    context("authenticate(user, rawPassword) 메서드는") {
        context("사용자 상태 검증") {
            test("ACTIVE 상태의 사용자는 인증을 통과할 수 있다") {
                // given
                val rawPassword = RawPassword("Password123!")
                val user =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = passwordEncryptor.encode(rawPassword),
                        nickname = Nickname("testuser"),
                        status = UserStatus.ACTIVE,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                shouldNotThrow<UserDomainException> {
                    userAuthenticator.authenticate(user, rawPassword)
                }
            }

            test("PENDING 상태의 사용자는 USER_PENDING 예외를 발생시킨다") {
                // given
                val rawPassword = RawPassword("Password123!")
                val user =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = passwordEncryptor.encode(rawPassword),
                        nickname = Nickname("testuser"),
                        status = UserStatus.PENDING,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, rawPassword)
                    }
                exception.userErrorCode shouldBe UserErrorCode.USER_PENDING
            }

            test("BANNED 상태의 사용자는 USER_BANNED 예외를 발생시킨다") {
                // given
                val rawPassword = RawPassword("Password123!")
                val user =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = passwordEncryptor.encode(rawPassword),
                        nickname = Nickname("testuser"),
                        status = UserStatus.BANNED,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, rawPassword)
                    }
                exception.userErrorCode shouldBe UserErrorCode.USER_BANNED
            }

            test("DELETED 상태의 사용자는 USER_DELETED 예외를 발생시킨다") {
                // given
                val rawPassword = RawPassword("Password123!")
                val user =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = passwordEncryptor.encode(rawPassword),
                        nickname = Nickname("testuser"),
                        status = UserStatus.DELETED,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, rawPassword)
                    }
                exception.userErrorCode shouldBe UserErrorCode.USER_DELETED
            }

            test("DORMANT 상태의 사용자는 정상적으로 인증을 통과할 수 있다") {
                // given
                val rawPassword = RawPassword("Password123!")
                val user =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = passwordEncryptor.encode(rawPassword),
                        nickname = Nickname("testuser"),
                        status = UserStatus.DORMANT,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                shouldNotThrow<UserDomainException> {
                    userAuthenticator.authenticate(user, rawPassword)
                }
            }
        }

        context("비밀번호 검증") {
            test("올바른 비밀번호로 인증 시 성공한다") {
                // given
                val rawPassword = RawPassword("CorrectPassword123!")
                val user =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = passwordEncryptor.encode(rawPassword),
                        nickname = Nickname("testuser"),
                        status = UserStatus.ACTIVE,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                shouldNotThrow<UserDomainException> {
                    userAuthenticator.authenticate(user, rawPassword)
                }
            }

            test("잘못된 비밀번호로 인증 시 INVALID_CREDENTIALS 예외를 발생시킨다") {
                // given
                val correctPassword = RawPassword("CorrectPassword123!")
                val wrongPassword = RawPassword("WrongPassword123!")
                val user =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = passwordEncryptor.encode(correctPassword),
                        nickname = Nickname("testuser"),
                        status = UserStatus.ACTIVE,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, wrongPassword)
                    }
                exception.userErrorCode shouldBe UserErrorCode.INVALID_CREDENTIALS
            }

            test("대소문자가 다른 비밀번호는 INVALID_CREDENTIALS 예외를 발생시킨다") {
                // given
                val correctPassword = RawPassword("Password123!")
                val wrongPassword = RawPassword("pAssword123!")
                val user =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("test@example.com"),
                        encodedPassword = passwordEncryptor.encode(correctPassword),
                        nickname = Nickname("testuser"),
                        status = UserStatus.ACTIVE,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, wrongPassword)
                    }
                exception.userErrorCode shouldBe UserErrorCode.INVALID_CREDENTIALS
            }

            test("빈 비밀번호로 인증 시 BLANK_PASSWORD 예외를 발생시킨다") {
                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        RawPassword("Password123!")
                        RawPassword("")
                    }
                exception.userErrorCode shouldBe UserErrorCode.BLANK_PASSWORD
            }
        }

        context("다양한 권한의 사용자 인증") {
            test("ADMIN 권한 사용자도 정상적으로 인증할 수 있다") {
                // given
                val rawPassword = RawPassword("AdminPassword123!")
                val adminUser =
                    User.create(
                        id = UserId(6000001L),
                        email = Email("admin@example.com"),
                        encodedPassword = passwordEncryptor.encode(rawPassword),
                        nickname = Nickname("admin"),
                        status = UserStatus.ACTIVE,
                        role = UserRole.ADMIN,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                shouldNotThrow<UserDomainException> {
                    userAuthenticator.authenticate(adminUser, rawPassword)
                }
            }

            test("USER 권한 사용자도 정상적으로 인증할 수 있다") {
                // given
                val rawPassword = RawPassword("UserPassword123!")
                val normalUser =
                    User.create(
                        id = UserId(1000001L),
                        email = Email("user@example.com"),
                        encodedPassword = passwordEncryptor.encode(rawPassword),
                        nickname = Nickname("user"),
                        status = UserStatus.ACTIVE,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // when & then
                shouldNotThrow<UserDomainException> {
                    userAuthenticator.authenticate(normalUser, rawPassword)
                }
            }
        }
    }

    context("authenticate(user, refreshToken) 메서드는") {
        context("사용자 상태 검증") {
            test("ACTIVE 상태의 사용자는 리프레시 토큰으로 인증할 수 있다") {
                // given
                val userId = UserId(1000001L)
                val refreshToken = "valid-refresh-token"
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
                tokenManager.addRefreshToken(refreshToken, userId)

                // when
                val result = userAuthenticator.authenticate(user, refreshToken)

                // then
                result shouldBe userId
            }

            test("PENDING 상태의 사용자는 USER_PENDING 예외를 발생시킨다") {
                // given
                val userId = UserId(1000001L)
                val refreshToken = "valid-refresh-token"
                val user =
                    User.create(
                        id = userId,
                        email = Email("test@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("testuser"),
                        status = UserStatus.PENDING,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )
                tokenManager.addRefreshToken(refreshToken, userId)

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, refreshToken)
                    }
                exception.userErrorCode shouldBe UserErrorCode.USER_PENDING
            }

            test("BANNED 상태의 사용자는 USER_BANNED 예외를 발생시킨다") {
                // given
                val userId = UserId(1000001L)
                val refreshToken = "valid-refresh-token"
                val user =
                    User.create(
                        id = userId,
                        email = Email("test@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("testuser"),
                        status = UserStatus.BANNED,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )
                tokenManager.addRefreshToken(refreshToken, userId)

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, refreshToken)
                    }
                exception.userErrorCode shouldBe UserErrorCode.USER_BANNED
            }

            test("DELETED 상태의 사용자는 USER_DELETED 예외를 발생시킨다") {
                // given
                val userId = UserId(1000001L)
                val refreshToken = "valid-refresh-token"
                val user =
                    User.create(
                        id = userId,
                        email = Email("test@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("testuser"),
                        status = UserStatus.DELETED,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )
                tokenManager.addRefreshToken(refreshToken, userId)

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, refreshToken)
                    }
                exception.userErrorCode shouldBe UserErrorCode.USER_DELETED
            }
        }

        context("리프레시 토큰 검증") {
            test("유효한 리프레시 토큰으로 인증 시 사용자 ID를 반환한다") {
                // given
                val userId = UserId(1000001L)
                val refreshToken = "valid-refresh-token"
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
                tokenManager.addRefreshToken(refreshToken, userId)

                // when
                val result = userAuthenticator.authenticate(user, refreshToken)

                // then
                result shouldBe userId
            }

            test("존재하지 않는 리프레시 토큰으로 인증 시 INVALID_TOKEN 예외를 발생시킨다") {
                // given
                val userId = UserId(1000001L)
                val invalidToken = "invalid-refresh-token"
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

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, invalidToken)
                    }
                exception.userErrorCode shouldBe UserErrorCode.INVALID_TOKEN
            }

            test("다른 사용자의 리프레시 토큰으로 인증 시 INVALID_TOKEN 예외를 발생시킨다") {
                // given
                val user1Id = UserId(1000001L)
                val user2Id = UserId(1000002L)
                val refreshToken = "refresh-token-for-user2"

                val user1 =
                    User.create(
                        id = user1Id,
                        email = Email("user1@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("user1"),
                        status = UserStatus.ACTIVE,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )

                // user2의 토큰을 등록
                tokenManager.addRefreshToken(refreshToken, user2Id)

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user1, refreshToken)
                    }
                exception.userErrorCode shouldBe UserErrorCode.INVALID_TOKEN
            }

            test("빈 리프레시 토큰으로 인증 시 INVALID_TOKEN 예외를 발생시킨다") {
                // given
                val userId = UserId(1000001L)
                val emptyToken = ""
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

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userAuthenticator.authenticate(user, emptyToken)
                    }
                exception.userErrorCode shouldBe UserErrorCode.INVALID_TOKEN
            }
        }

        context("다양한 권한의 사용자 리프레시 토큰 인증") {
            test("ADMIN 권한 사용자도 리프레시 토큰으로 인증할 수 있다") {
                // given
                val adminId = UserId(6000001L)
                val refreshToken = "admin-refresh-token"
                val adminUser =
                    User.create(
                        id = adminId,
                        email = Email("admin@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("admin"),
                        status = UserStatus.ACTIVE,
                        role = UserRole.ADMIN,
                        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                    )
                tokenManager.addRefreshToken(refreshToken, adminId)

                // when
                val result = userAuthenticator.authenticate(adminUser, refreshToken)

                // then
                result shouldBe adminId
            }
        }
    }

    context("격리성 테스트") {
        test("각 인증 시도는 독립적으로 수행된다") {
            // given
            val user1 =
                User.create(
                    id = UserId(1000001L),
                    email = Email("user1@example.com"),
                    encodedPassword = passwordEncryptor.encode(RawPassword("Password1!")),
                    nickname = Nickname("user1"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )

            val user2 =
                User.create(
                    id = UserId(1000002L),
                    email = Email("user2@example.com"),
                    encodedPassword = passwordEncryptor.encode(RawPassword("Password2!")),
                    nickname = Nickname("user2"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )

            // when & then
            // user1 인증 성공
            shouldNotThrow<UserDomainException> {
                userAuthenticator.authenticate(user1, RawPassword("Password1!"))
            }

            // user2 인증 실패 (잘못된 비밀번호)
            shouldThrow<UserDomainException> {
                userAuthenticator.authenticate(user2, RawPassword("WrongPassword!"))
            }

            // user1 다시 인증 성공 (user2의 실패가 영향을 주지 않음)
            shouldNotThrow<UserDomainException> {
                userAuthenticator.authenticate(user1, RawPassword("Password1!"))
            }
        }

        test("비밀번호 인증과 토큰 인증은 서로 독립적이다") {
            // given
            val userId = UserId(1000001L)
            val refreshToken = "refresh-token"
            val user =
                User.create(
                    id = userId,
                    email = Email("test@example.com"),
                    encodedPassword = passwordEncryptor.encode(RawPassword("Password123!")),
                    nickname = Nickname("testuser"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    modifiedAt = Instant.parse("2025-01-01T00:00:00Z"),
                )
            tokenManager.addRefreshToken(refreshToken, userId)

            // when & then
            // 비밀번호 인증 실패
            shouldThrow<UserDomainException> {
                userAuthenticator.authenticate(user, RawPassword("WrongPassword!"))
            }

            // 토큰 인증은 여전히 성공
            val result = userAuthenticator.authenticate(user, refreshToken)
            result shouldBe userId

            // 비밀번호 인증 성공
            shouldNotThrow<UserDomainException> {
                userAuthenticator.authenticate(user, RawPassword("Password123!"))
            }
        }
    }
})
