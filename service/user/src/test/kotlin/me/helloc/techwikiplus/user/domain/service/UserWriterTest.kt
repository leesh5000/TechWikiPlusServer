package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.FakeClock
import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.service.fake.FakeUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserWriterTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var userWriter: UserWriter
    private lateinit var clock: FakeClock

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userWriter = UserWriter(userRepository)
        clock = FakeClock(LocalDateTime.of(2024, 1, 1, 12, 0))
    }

    @Test
    fun `insertOrUpdate should insert new user`() {
        // given
        val newUser = User(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "testuser",
            password = "encoded_password",
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )

        // when
        userWriter.insertOrUpdate(newUser)

        // then
        val savedUser = userRepository.findByEmail("test@example.com")
        assertThat(savedUser).isNotNull
        assertThat(savedUser).isEqualTo(newUser)
        assertThat(savedUser?.nickname).isEqualTo("testuser")
    }

    @Test
    fun `insertOrUpdate should update existing user`() {
        // given
        val existingUser = User(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "oldnickname",
            password = "old_password",
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )
        userRepository.insertOrUpdate(existingUser)

        val updatedUser = existingUser.copy(
            nickname = "newnickname",
            password = "new_password",
            clock = clock
        )

        // when
        userWriter.insertOrUpdate(updatedUser)

        // then
        val savedUser = userRepository.findByEmail("test@example.com")
        assertThat(savedUser).isNotNull
        assertThat(savedUser?.nickname).isEqualTo("newnickname")
        assertThat(savedUser?.password).isEqualTo("new_password")
    }

    @Test
    fun `insertOrUpdate should handle multiple users`() {
        // given
        val user1 = User(
            id = 1L,
            email = UserEmail("user1@example.com"),
            nickname = "user1",
            password = "password1",
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )
        
        val user2 = User(
            id = 2L,
            email = UserEmail("user2@example.com"),
            nickname = "user2",
            password = "password2",
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )

        // when
        userWriter.insertOrUpdate(user1)
        userWriter.insertOrUpdate(user2)

        // then
        assertThat(userRepository.findAll()).hasSize(2)
        assertThat(userRepository.findByEmail("user1@example.com")).isEqualTo(user1)
        assertThat(userRepository.findByEmail("user2@example.com")).isEqualTo(user2)
    }

    @Test
    fun `insertOrUpdate should correctly update user status`() {
        // given
        val pendingUser = User.withPendingUser(
            id = 1L,
            email = UserEmail("test@example.com"),
            nickname = "testuser",
            password = "encoded_password",
            clock = clock
        )
        userRepository.insertOrUpdate(pendingUser)

        val activeUser = pendingUser.copy(
            status = UserStatus.ACTIVE,
            clock = clock
        )

        // when
        userWriter.insertOrUpdate(activeUser)

        // then
        val savedUser = userRepository.findByEmail("test@example.com")
        assertThat(savedUser).isNotNull
        assertThat(savedUser?.status).isEqualTo(UserStatus.ACTIVE)
        assertThat(savedUser?.id).isEqualTo(1L)
    }

    @Test
    fun `insertOrUpdate should update email index when email changes`() {
        // given
        val originalEmail = "original@example.com"
        val newEmail = "new@example.com"
        
        val user = User(
            id = 1L,
            email = UserEmail(originalEmail),
            nickname = "testuser",
            password = "encoded_password",
            createdAt = clock.localDateTime(),
            updatedAt = clock.localDateTime()
        )
        userRepository.insertOrUpdate(user)

        val userWithNewEmail = user.copy(
            email = UserEmail(newEmail),
            clock = clock
        )

        // when
        userWriter.insertOrUpdate(userWithNewEmail)

        // then
        assertThat(userRepository.findByEmail(originalEmail)).isNull()
        assertThat(userRepository.findByEmail(newEmail)).isNotNull
        assertThat(userRepository.findByEmail(newEmail)?.id).isEqualTo(1L)
    }
}