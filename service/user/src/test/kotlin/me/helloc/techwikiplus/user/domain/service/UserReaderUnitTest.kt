package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.exception.CustomException.NotFoundException.UserEmailNotFoundException
import me.helloc.techwikiplus.user.infrastructure.clock.fake.FakeClock
import me.helloc.techwikiplus.user.infrastructure.persistence.fake.FakeUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserReaderUnitTest {
    private lateinit var userRepository: FakeUserRepository
    private lateinit var userReader: UserReader
    private lateinit var clock: FakeClock

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userReader = UserReader(userRepository)
        clock = FakeClock(LocalDateTime.of(2024, 1, 1, 12, 0))
    }

    @Test
    fun `readByEmailOrThrows should return user when user exists`() {
        // given
        val email = "test@example.com"
        val existingUser =
            User(
                id = 1L,
                email = UserEmail(email),
                nickname = "testuser",
                password = "encoded_password",
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )
        userRepository.insertOrUpdate(existingUser)

        // when
        val result = userReader.readByEmailOrThrows(email)

        // then
        assertThat(result).isEqualTo(existingUser)
        assertThat(result.email()).isEqualTo(email)
        assertThat(result.nickname).isEqualTo("testuser")
    }

    @Test
    fun `readByEmailOrThrows should throw UserEmailNotFoundException when user does not exist`() {
        // given
        val nonExistentEmail = "nonexistent@example.com"

        // when & then
        assertThatThrownBy { userReader.readByEmailOrThrows(nonExistentEmail) }
            .isInstanceOf(UserEmailNotFoundException::class.java)
            .hasMessage("User not found with email: $nonExistentEmail")
    }

    @Test
    fun `readByEmailOrThrows should return correct user when multiple users exist`() {
        // given
        val email1 = "user1@example.com"
        val email2 = "user2@example.com"

        val user1 =
            User(
                id = 1L,
                email = UserEmail(email1),
                nickname = "user1",
                password = "encoded_password1",
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )

        val user2 =
            User(
                id = 2L,
                email = UserEmail(email2),
                nickname = "user2",
                password = "encoded_password2",
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )

        userRepository.insertOrUpdate(user1)
        userRepository.insertOrUpdate(user2)

        // when
        val result = userReader.readByEmailOrThrows(email2)

        // then
        assertThat(result).isEqualTo(user2)
        assertThat(result.email()).isEqualTo(email2)
        assertThat(result.nickname).isEqualTo("user2")
    }

    @Test
    fun `readByEmailOrThrows should handle email case sensitivity correctly`() {
        // given
        val email = "Test@Example.com"
        val existingUser =
            User(
                id = 1L,
                email = UserEmail(email),
                nickname = "testuser",
                password = "encoded_password",
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )
        userRepository.insertOrUpdate(existingUser)

        // when
        val result = userReader.readByEmailOrThrows(email)

        // then
        assertThat(result).isEqualTo(existingUser)
    }
}
