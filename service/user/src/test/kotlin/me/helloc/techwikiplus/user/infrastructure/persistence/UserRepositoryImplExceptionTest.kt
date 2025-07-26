package me.helloc.techwikiplus.user.infrastructure.persistence

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.infrastructure.exception.DataAccessException
import me.helloc.techwikiplus.user.infrastructure.persistence.jpa.UserEntity
import me.helloc.techwikiplus.user.infrastructure.persistence.jpa.UserJpaRepository
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

@DisplayName("UserRepositoryImpl 예외 변환 테스트")
class UserRepositoryImplExceptionTest {
    private lateinit var jpaRepository: UserJpaRepository
    private lateinit var repository: UserRepositoryImpl

    @BeforeEach
    fun setUp() {
        jpaRepository = mock(UserJpaRepository::class.java)
        repository = UserRepositoryImpl(jpaRepository)
    }

    @Test
    @DisplayName("existsByEmail에서 JPA 예외 발생 시 DataAccessException으로 변환")
    fun `converts JPA exception to DataAccessException on existsByEmail`() {
        // given
        val email = "user@example.com"
        val jpaException = RuntimeException("Database connection failed")
        `when`(jpaRepository.existsByEmail(email)).thenThrow(jpaException)

        // when & then
        assertThatThrownBy { repository.existsByEmail(email) }
            .isInstanceOf(DataAccessException::class.java)
            .hasMessageContaining("checking email existence")
            .hasCause(jpaException)
            .extracting("retryable")
            .isEqualTo(false)
    }

    @Test
    @DisplayName("existsByNickname에서 JPA 예외 발생 시 DataAccessException으로 변환")
    fun `converts JPA exception to DataAccessException on existsByNickname`() {
        // given
        val nickname = "testuser"
        val jpaException = RuntimeException("Query timeout")
        `when`(jpaRepository.existsByNickname(nickname)).thenThrow(jpaException)

        // when & then
        assertThatThrownBy { repository.existsByNickname(nickname) }
            .isInstanceOf(DataAccessException::class.java)
            .hasMessageContaining("checking nickname existence")
            .hasCause(jpaException)
    }

    @Test
    @DisplayName("insertOrUpdate에서 JPA 예외 발생 시 DataAccessException으로 변환")
    fun `converts JPA exception to DataAccessException on insertOrUpdate`() {
        // given
        val user = createTestUser()
        val jpaException = RuntimeException("Constraint violation")
        `when`(jpaRepository.save(any(UserEntity::class.java))).thenThrow(jpaException)

        // when & then
        assertThatThrownBy { repository.insertOrUpdate(user) }
            .isInstanceOf(DataAccessException::class.java)
            .hasMessageContaining("saving user")
            .hasCause(jpaException)
    }

    @Test
    @DisplayName("findByEmail에서 JPA 예외 발생 시 DataAccessException으로 변환")
    fun `converts JPA exception to DataAccessException on findByEmail`() {
        // given
        val email = "user@example.com"
        val jpaException = RuntimeException("Index corrupted")
        `when`(jpaRepository.findByEmail(email)).thenThrow(jpaException)

        // when & then
        assertThatThrownBy { repository.findByEmail(email) }
            .isInstanceOf(DataAccessException::class.java)
            .hasMessageContaining("finding user by email")
            .hasCause(jpaException)
    }

    private fun createTestUser(): User {
        return User(
            id = 1L,
            email = UserEmail("user@example.com"),
            password = "hashedPassword",
            nickname = "testuser",
            status = UserStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
    }
}
