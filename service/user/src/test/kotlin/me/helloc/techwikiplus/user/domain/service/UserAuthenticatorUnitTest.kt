package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.exception.authentication.AccountBannedException
import me.helloc.techwikiplus.user.domain.exception.authentication.AccountDeletedException
import me.helloc.techwikiplus.user.domain.exception.authentication.AccountDormantException
import me.helloc.techwikiplus.user.domain.exception.authentication.EmailNotVerifiedException
import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidCredentialsException
import me.helloc.techwikiplus.user.infrastructure.clock.fake.FakeClock
import me.helloc.techwikiplus.user.infrastructure.passwordencoder.fake.FakePasswordEncoder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserAuthenticatorUnitTest {
    private lateinit var passwordEncoder: FakePasswordEncoder
    private lateinit var userAuthenticator: UserAuthenticator
    private lateinit var clock: FakeClock

    @BeforeEach
    fun setUp() {
        passwordEncoder = FakePasswordEncoder()
        userAuthenticator = UserAuthenticator(passwordEncoder)
        clock = FakeClock(LocalDateTime.of(2024, 1, 1, 12, 0))
    }

    @Test
    @DisplayName("비밀번호가 유효하고 사용자가 활성 상태일 때 사용자 반환")
    fun `비밀번호가 유효하고 사용자가 활성 상태일 때 사용자 반환`() {
        // given
        val rawPassword = "password123"
        val encodedPassword = passwordEncoder.encode(rawPassword)
        val activeUser =
            User(
                id = 1L,
                email = UserEmail("test@example.com"),
                nickname = "testuser",
                password = encodedPassword,
                status = UserStatus.ACTIVE,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )

        // when
        val result = userAuthenticator.authenticate(activeUser, rawPassword)

        // then
        assertThat(result).isEqualTo(activeUser)
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않을 때 InvalidCredentials 예외 발생")
    fun `비밀번호가 일치하지 않을 때 InvalidCredentials 예외 발생`() {
        // given
        val wrongPassword = "wrongpassword"
        val activeUser =
            User(
                id = 1L,
                email = UserEmail("test@example.com"),
                nickname = "testuser",
                password = passwordEncoder.encode("correctpassword"),
                status = UserStatus.ACTIVE,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )

        // when & then
        assertThatThrownBy { userAuthenticator.authenticate(activeUser, wrongPassword) }
            .isInstanceOf(InvalidCredentialsException::class.java)
            .hasMessage("Invalid email or password")
    }

    @Test
    @DisplayName("사용자 상태가 PENDING일 때 EmailNotVerified 예외 발생")
    fun `사용자 상태가 PENDING일 때 EmailNotVerified 예외 발생`() {
        // given
        val rawPassword = "password123"
        val pendingUser =
            User(
                id = 1L,
                email = UserEmail("test@example.com"),
                nickname = "testuser",
                password = passwordEncoder.encode(rawPassword),
                status = UserStatus.PENDING,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )

        // when & then
        assertThatThrownBy { userAuthenticator.authenticate(pendingUser, rawPassword) }
            .isInstanceOf(EmailNotVerifiedException::class.java)
            .hasMessage("Email not verified. Details: Please verify your email before logging in.")
    }

    @Test
    @DisplayName("사용자 상태가 BANNED일 때 AccountBanned 예외 발생")
    fun `사용자 상태가 BANNED일 때 AccountBanned 예외 발생`() {
        // given
        val rawPassword = "password123"
        val bannedUser =
            User(
                id = 1L,
                email = UserEmail("test@example.com"),
                nickname = "testuser",
                password = passwordEncoder.encode(rawPassword),
                status = UserStatus.BANNED,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )

        // when & then
        assertThatThrownBy { userAuthenticator.authenticate(bannedUser, rawPassword) }
            .isInstanceOf(AccountBannedException::class.java)
            .hasMessage("Account has been banned. Details: Your account has been banned.")
    }

    @Test
    @DisplayName("사용자 상태가 DORMANT일 때 AccountDormant 예외 발생")
    fun `사용자 상태가 DORMANT일 때 AccountDormant 예외 발생`() {
        // given
        val rawPassword = "password123"
        val dormantUser =
            User(
                id = 1L,
                email = UserEmail("test@example.com"),
                nickname = "testuser",
                password = passwordEncoder.encode(rawPassword),
                status = UserStatus.DORMANT,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )

        // when & then
        assertThatThrownBy { userAuthenticator.authenticate(dormantUser, rawPassword) }
            .isInstanceOf(AccountDormantException::class.java)
            .hasMessage("Account is dormant. Details: Your account is dormant. Please contact support to reactivate.")
    }

    @Test
    @DisplayName("사용자 상태가 DELETED일 때 AccountDeleted 예외 발생")
    fun `사용자 상태가 DELETED일 때 AccountDeleted 예외 발생`() {
        // given
        val rawPassword = "password123"
        val deletedUser =
            User(
                id = 1L,
                email = UserEmail("test@example.com"),
                nickname = "testuser",
                password = passwordEncoder.encode(rawPassword),
                status = UserStatus.DELETED,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )

        // when & then
        assertThatThrownBy { userAuthenticator.authenticate(deletedUser, rawPassword) }
            .isInstanceOf(AccountDeletedException::class.java)
            .hasMessage("Account has been deleted. Details: Your account has been deleted.")
    }

    @Test
    @DisplayName("사용자 상태 확인 전에 비밀번호 검증을 먼저 수행")
    fun `사용자 상태 확인 전에 비밀번호 검증을 먼저 수행`() {
        // given
        val wrongPassword = "wrongpassword"
        val pendingUser =
            User(
                id = 1L,
                email = UserEmail("test@example.com"),
                nickname = "testuser",
                password = passwordEncoder.encode("correctpassword"),
                status = UserStatus.PENDING,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )

        // when & then
        assertThatThrownBy { userAuthenticator.authenticate(pendingUser, wrongPassword) }
            .isInstanceOf(InvalidCredentialsException::class.java)
            .hasMessage("Invalid email or password")
    }
}
