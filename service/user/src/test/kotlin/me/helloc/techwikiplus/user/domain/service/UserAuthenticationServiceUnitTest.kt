package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.infrastructure.clock.fake.FakeClock
import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.infrastructure.passwordencoder.fake.FakeUserPasswordService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserAuthenticationServiceUnitTest {

    private lateinit var userPasswordService: FakeUserPasswordService
    private lateinit var userAuthenticationService: UserAuthenticationService
    private lateinit var clock: FakeClock

    @BeforeEach
    fun setUp() {
        userPasswordService = FakeUserPasswordService()
        userAuthenticationService = UserAuthenticationService(userPasswordService)
        clock = FakeClock(LocalDateTime.of(2024, 1, 1, 12, 0))
    }

    @Test
    fun `authenticate should return user when password is valid and user is active`() {
        // given
        val rawPassword = "password123"
        val encodedPassword = userPasswordService.encode(rawPassword)
        val activeUser = User(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "testuser",
            password = encodedPassword,
            status = UserStatus.ACTIVE,
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )

        // when
        val result = userAuthenticationService.authenticate(activeUser, rawPassword)

        // then
        assertThat(result).isEqualTo(activeUser)
    }

    @Test
    fun `authenticate should throw InvalidCredentials when password does not match`() {
        // given
        val wrongPassword = "wrongpassword"
        val activeUser = User(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "testuser",
            password = userPasswordService.encode("correctpassword"),
            status = UserStatus.ACTIVE,
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )

        // when & then
        assertThatThrownBy { userAuthenticationService.authenticate(activeUser, wrongPassword) }
            .isInstanceOf(CustomException.AuthenticationException.InvalidCredentials::class.java)
            .hasMessage("Invalid email or password")
    }

    @Test
    fun `authenticate should throw EmailNotVerified when user status is PENDING`() {
        // given
        val rawPassword = "password123"
        val pendingUser = User(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "testuser",
            password = userPasswordService.encode(rawPassword),
            status = UserStatus.PENDING,
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )

        // when & then
        assertThatThrownBy { userAuthenticationService.authenticate(pendingUser, rawPassword) }
            .isInstanceOf(CustomException.AuthenticationException.EmailNotVerified::class.java)
            .hasMessage("Email not verified. Please verify your email before logging in.")
    }

    @Test
    fun `authenticate should throw AccountBanned when user status is BANNED`() {
        // given
        val rawPassword = "password123"
        val bannedUser = User(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "testuser",
            password = userPasswordService.encode(rawPassword),
            status = UserStatus.BANNED,
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )

        // when & then
        assertThatThrownBy { userAuthenticationService.authenticate(bannedUser, rawPassword) }
            .isInstanceOf(CustomException.AuthenticationException.AccountBanned::class.java)
            .hasMessage("Your account has been banned.")
    }

    @Test
    fun `authenticate should throw AccountDormant when user status is DORMANT`() {
        // given
        val rawPassword = "password123"
        val dormantUser = User(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "testuser",
            password = userPasswordService.encode(rawPassword),
            status = UserStatus.DORMANT,
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )

        // when & then
        assertThatThrownBy { userAuthenticationService.authenticate(dormantUser, rawPassword) }
            .isInstanceOf(CustomException.AuthenticationException.AccountDormant::class.java)
            .hasMessage("Your account is dormant. Please contact support to reactivate.")
    }

    @Test
    fun `authenticate should throw AccountDeleted when user status is DELETED`() {
        // given
        val rawPassword = "password123"
        val deletedUser = User(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "testuser",
            password = userPasswordService.encode(rawPassword),
            status = UserStatus.DELETED,
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )

        // when & then
        assertThatThrownBy { userAuthenticationService.authenticate(deletedUser, rawPassword) }
            .isInstanceOf(CustomException.AuthenticationException.AccountDeleted::class.java)
            .hasMessage("Your account has been deleted.")
    }

    @Test
    fun `authenticate should validate password first before checking user status`() {
        // given
        val wrongPassword = "wrongpassword"
        val pendingUser = User(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "testuser",
            password = userPasswordService.encode("correctpassword"),
            status = UserStatus.PENDING,
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )

        // when & then
        assertThatThrownBy { userAuthenticationService.authenticate(pendingUser, wrongPassword) }
            .isInstanceOf(CustomException.AuthenticationException.InvalidCredentials::class.java)
            .hasMessage("Invalid email or password")
    }
}
