package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.exception.authentication.PendingUserNotFoundException
import me.helloc.techwikiplus.user.infrastructure.clock.fake.FakeClock
import me.helloc.techwikiplus.user.infrastructure.persistence.fake.FakeUserRepository
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PendingUserValidatorUnitTest {
    private lateinit var userRepository: FakeUserRepository
    private lateinit var pendingUserValidator: PendingUserValidator
    private lateinit var clock: FakeClock

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        pendingUserValidator = PendingUserValidator(userRepository)
        clock = FakeClock(LocalDateTime.of(2024, 1, 1, 12, 0))
    }

    @Test
    fun `existsOrThrows should not throw when pending user exists`() {
        // given
        val email = "test@example.com"
        val pendingUser =
            User.withPendingUser(
                id = 1L,
                email = UserEmail(email),
                nickname = "testuser",
                password = "encoded_password",
                clock = clock,
            )
        userRepository.insertOrUpdate(pendingUser)

        // when & then
        assertThatNoException()
            .isThrownBy { pendingUserValidator.existsOrThrows(email) }
    }

    @Test
    fun `existsOrThrows should throw PendingUserNotFound when user does not exist`() {
        // given
        val email = "nonexistent@example.com"

        // when & then
        assertThatThrownBy { pendingUserValidator.existsOrThrows(email) }
            .isInstanceOf(PendingUserNotFoundException::class.java)
            .hasMessage(
                "Pending user not found. Details: Pending user not found for email: $email. " +
                    "Please ensure you have registered and requested verification.",
            )
    }

    @Test
    fun `existsOrThrows should throw PendingUserNotFound when user exists but not pending`() {
        // given
        val email = "active@example.com"
        val activeUser =
            User(
                id = 1L,
                email = UserEmail(email),
                nickname = "activeuser",
                password = "encoded_password",
                status = UserStatus.ACTIVE,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )
        userRepository.insertOrUpdate(activeUser)

        // when & then
        assertThatThrownBy { pendingUserValidator.existsOrThrows(email) }
            .isInstanceOf(PendingUserNotFoundException::class.java)
            .hasMessage(
                "Pending user not found. Details: Pending user not found for email: $email. " +
                    "Please ensure you have registered and requested verification.",
            )
    }

    @Test
    fun `existsOrThrows should throw PendingUserNotFound when user is banned`() {
        // given
        val email = "banned@example.com"
        val bannedUser =
            User(
                id = 1L,
                email = UserEmail(email),
                nickname = "banneduser",
                password = "encoded_password",
                status = UserStatus.BANNED,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )
        userRepository.insertOrUpdate(bannedUser)

        // when & then
        assertThatThrownBy { pendingUserValidator.existsOrThrows(email) }
            .isInstanceOf(PendingUserNotFoundException::class.java)
            .hasMessage(
                "Pending user not found. Details: Pending user not found for email: $email. " +
                    "Please ensure you have registered and requested verification.",
            )
    }
}
