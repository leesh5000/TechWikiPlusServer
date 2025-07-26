package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.exception.conflict.DuplicateEmailException
import me.helloc.techwikiplus.user.domain.exception.conflict.DuplicateNicknameException
import me.helloc.techwikiplus.user.infrastructure.clock.fake.FakeClock
import me.helloc.techwikiplus.user.infrastructure.persistence.fake.FakeUserRepository
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserDuplicateCheckerUnitTest {
    private lateinit var userRepository: FakeUserRepository
    private lateinit var userDuplicateChecker: UserDuplicateChecker
    private lateinit var clock: FakeClock

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userDuplicateChecker = UserDuplicateChecker(userRepository)
        clock = FakeClock(LocalDateTime.of(2024, 1, 1, 12, 0))
    }

    @Test
    fun `validateUserEmailDuplicate should not throw when email does not exist`() {
        // given
        val email = "new@example.com"

        // when & then
        assertThatNoException()
            .isThrownBy { userDuplicateChecker.validateUserEmailDuplicate(email) }
    }

    @Test
    fun `validateUserEmailDuplicate should throw DuplicateEmail when email already exists`() {
        // given
        val email = "existing@example.com"
        val existingUser =
            User(
                id = 1L,
                email = UserEmail(email),
                nickname = "existinguser",
                password = "encoded_password",
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )
        userRepository.insertOrUpdate(existingUser)

        // when & then
        assertThatThrownBy { userDuplicateChecker.validateUserEmailDuplicate(email) }
            .isInstanceOf(DuplicateEmailException::class.java)
            .hasMessage("Email already exists. Details: Your input: $email")
    }

    @Test
    fun `validateUserNicknameDuplicate should not throw when nickname does not exist`() {
        // given
        val nickname = "newnickname"

        // when & then
        assertThatNoException()
            .isThrownBy { userDuplicateChecker.validateUserNicknameDuplicate(nickname) }
    }

    @Test
    fun `validateUserNicknameDuplicate should throw DuplicateNickname when nickname already exists`() {
        // given
        val nickname = "existingnickname"
        val existingUser =
            User(
                id = 1L,
                email = UserEmail("test@example.com"),
                nickname = nickname,
                password = "encoded_password",
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )
        userRepository.insertOrUpdate(existingUser)

        // when & then
        assertThatThrownBy { userDuplicateChecker.validateUserNicknameDuplicate(nickname) }
            .isInstanceOf(DuplicateNicknameException::class.java)
            .hasMessage("Nickname already exists. Details: Your input: $nickname")
    }

    @Test
    fun `validateUserEmailDuplicate should check exact email match`() {
        // given
        val existingEmail = "test@example.com"
        val newEmail = "test2@example.com"
        val existingUser =
            User(
                id = 1L,
                email = UserEmail(existingEmail),
                nickname = "testuser",
                password = "encoded_password",
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )
        userRepository.insertOrUpdate(existingUser)

        // when & then
        assertThatNoException()
            .isThrownBy { userDuplicateChecker.validateUserEmailDuplicate(newEmail) }
    }

    @Test
    fun `validateUserNicknameDuplicate should check exact nickname match`() {
        // given
        val existingNickname = "testuser"
        val newNickname = "testuser2"
        val existingUser =
            User(
                id = 1L,
                email = UserEmail("test@example.com"),
                nickname = existingNickname,
                password = "encoded_password",
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )
        userRepository.insertOrUpdate(existingUser)

        // when & then
        assertThatNoException()
            .isThrownBy { userDuplicateChecker.validateUserNicknameDuplicate(newNickname) }
    }
}
