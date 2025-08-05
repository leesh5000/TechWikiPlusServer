package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.BannedUserException
import me.helloc.techwikiplus.service.user.domain.exception.DormantUserException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.exception.PendingUserException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordCrypter
import java.time.Instant

class UserAuthenticatorUnitTest : FunSpec({
    lateinit var userAuthenticator: UserAuthenticator
    lateinit var fakePasswordCrypter: FakePasswordCrypter

    beforeEach {
        fakePasswordCrypter = FakePasswordCrypter()
        userAuthenticator = UserAuthenticator(fakePasswordCrypter)
    }

    context("authenticateOrThrows 메서드") {
        val correctPassword = RawPassword("Password123!")
        val wrongPassword = RawPassword("wrongPassword!")
        val encodedPassword = EncodedPassword("FAKE_ENCODED:Password123!")
        val now = Instant.now()

        test("ACTIVE 상태의 사용자가 올바른 비밀번호로 인증할 때 예외를 던지지 않는다") {
            // Given
            val activeUser =
                User.create(
                    id = "user-1",
                    email = Email("active@example.com"),
                    nickname = Nickname("activeuser"),
                    encodedPassword = encodedPassword,
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = now,
                )

            // When & Then
            shouldNotThrow<Exception> {
                userAuthenticator.authenticateOrThrows(activeUser, correctPassword)
            }
        }

        test("ACTIVE 상태의 사용자가 잘못된 비밀번호로 인증할 때 InvalidCredentialsException을 던진다") {
            // Given
            val activeUser =
                User.create(
                    id = "user-1",
                    email = Email("active@example.com"),
                    nickname = Nickname("activeuser"),
                    encodedPassword = encodedPassword,
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = now,
                )

            // When & Then
            shouldThrow<InvalidCredentialsException> {
                userAuthenticator.authenticateOrThrows(activeUser, wrongPassword)
            }
        }

        test("PENDING 상태의 사용자가 인증을 시도할 때 PendingUserException을 던진다") {
            // Given
            val pendingUser =
                User.create(
                    id = "user-2",
                    email = Email("pending@example.com"),
                    nickname = Nickname("pendinguser"),
                    encodedPassword = encodedPassword,
                    status = UserStatus.PENDING,
                    role = UserRole.USER,
                    createdAt = now,
                )

            // When & Then
            shouldThrow<PendingUserException> {
                userAuthenticator.authenticateOrThrows(pendingUser, correctPassword)
            }
        }

        test("DORMANT 상태의 사용자가 인증을 시도할 때 DormantUserException을 던진다") {
            // Given
            val dormantUser =
                User.create(
                    id = "user-3",
                    email = Email("dormant@example.com"),
                    nickname = Nickname("dormantuser"),
                    encodedPassword = encodedPassword,
                    status = UserStatus.DORMANT,
                    role = UserRole.USER,
                    createdAt = now,
                )

            // When & Then
            shouldThrow<DormantUserException> {
                userAuthenticator.authenticateOrThrows(dormantUser, correctPassword)
            }
        }

        test("BANNED 상태의 사용자가 인증을 시도할 때 BannedUserException을 던진다") {
            // Given
            val bannedUser =
                User.create(
                    id = "user-4",
                    email = Email("banned@example.com"),
                    nickname = Nickname("banneduser"),
                    encodedPassword = encodedPassword,
                    status = UserStatus.BANNED,
                    role = UserRole.USER,
                    createdAt = now,
                )

            // When & Then
            shouldThrow<BannedUserException> {
                userAuthenticator.authenticateOrThrows(bannedUser, correctPassword)
            }
        }

        test("DELETED 상태의 사용자가 인증을 시도할 때 UserNotFoundException을 던진다") {
            // Given
            val deletedUser =
                User.create(
                    id = "user-5",
                    email = Email("deleted@example.com"),
                    nickname = Nickname("deleteduser"),
                    encodedPassword = encodedPassword,
                    status = UserStatus.DELETED,
                    role = UserRole.USER,
                    createdAt = now,
                )

            // When & Then
            val exception =
                shouldThrow<UserNotFoundException> {
                    userAuthenticator.authenticateOrThrows(deletedUser, correctPassword)
                }
            exception.message shouldBe "User not found: User has been deleted"
        }
    }
})
