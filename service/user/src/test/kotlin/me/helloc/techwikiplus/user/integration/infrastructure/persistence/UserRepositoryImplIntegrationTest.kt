package me.helloc.techwikiplus.user.integration.infrastructure.persistence

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserRole
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.infrastructure.config.IntegrationTestSupport
import me.helloc.techwikiplus.user.infrastructure.config.TestContainerConfig
import me.helloc.techwikiplus.user.infrastructure.persistence.UserRepositoryImpl
import me.helloc.techwikiplus.user.infrastructure.persistence.jpa.UserJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.LocalDateTime

class UserRepositoryImplIntegrationTest : IntegrationTestSupport() {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            TestContainerConfig.properties(registry)
        }
    }

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

    @Autowired
    private lateinit var jpaRepository: UserJpaRepository

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 데이터 초기화
        jpaRepository.deleteAll()
    }

    @Nested
    @DisplayName("이메일 중복 체크")
    inner class EmailDuplicationCheckTest {
        @Test
        fun `존재하는 이메일은 true를 반환한다`() {
            // given
            val user =
                createTestUser(
                    id = 1L,
                    email = "existing@example.com",
                    nickname = "테스터1",
                )
            userRepository.insertOrUpdate(user)

            // when
            val exists = userRepository.existsByEmail("existing@example.com")

            // then
            assertThat(exists).isTrue
        }

        @Test
        fun `존재하지 않는 이메일은 false를 반환한다`() {
            // when
            val exists = userRepository.existsByEmail("nonexistent@example.com")

            // then
            assertThat(exists).isFalse
        }

        @Test
        fun `대소문자를 구분하지 않고 체크한다`() {
            // given
            val user =
                createTestUser(
                    id = 1L,
                    email = "test@example.com",
                    nickname = "테스터",
                )
            userRepository.insertOrUpdate(user)

            // when & then
            assertThat(userRepository.existsByEmail("test@example.com")).isTrue
            assertThat(userRepository.existsByEmail("Test@example.com")).isTrue
            assertThat(userRepository.existsByEmail("TEST@EXAMPLE.COM")).isTrue
        }
    }

    @Nested
    @DisplayName("닉네임 중복 체크")
    inner class NicknameDuplicationCheckTest {
        @Test
        fun `존재하는 닉네임은 true를 반환한다`() {
            // given
            val user =
                createTestUser(
                    id = 2L,
                    email = "user@example.com",
                    nickname = "기존닉네임",
                )
            userRepository.insertOrUpdate(user)

            // when
            val exists = userRepository.existsByNickname("기존닉네임")

            // then
            assertThat(exists).isTrue
        }

        @Test
        fun `존재하지 않는 닉네임은 false를 반환한다`() {
            // when
            val exists = userRepository.existsByNickname("새로운닉네임")

            // then
            assertThat(exists).isFalse
        }

        @Test
        fun `닉네임도 정확히 일치해야 한다`() {
            // given
            val user =
                createTestUser(
                    id = 3L,
                    email = "nick@example.com",
                    nickname = "테스터123",
                )
            userRepository.insertOrUpdate(user)

            // when & then
            assertThat(userRepository.existsByNickname("테스터123")).isTrue
            assertThat(userRepository.existsByNickname("테스터")).isFalse
            assertThat(userRepository.existsByNickname("테스터1234")).isFalse
        }
    }

    @Nested
    @DisplayName("사용자 저장")
    inner class UserSaveTest {
        @Test
        fun `새로운 사용자를 저장한다`() {
            // given
            val user =
                createTestUser(
                    id = 4L,
                    email = "new@example.com",
                    nickname = "새사용자",
                )

            // when
            userRepository.insertOrUpdate(user)

            // then
            val savedUser = userRepository.findByEmail("new@example.com")
            assertThat(savedUser).isNotNull
            assertThat(savedUser?.id).isEqualTo(4L)
            assertThat(savedUser?.getEmailValue()).isEqualTo("new@example.com")
            assertThat(savedUser?.nickname).isEqualTo("새사용자")
        }

        @Test
        fun `기존 사용자를 업데이트한다`() {
            // given
            val user =
                createTestUser(
                    id = 5L,
                    email = "update@example.com",
                    nickname = "원래닉네임",
                )
            userRepository.insertOrUpdate(user)

            // when
            val updatedUser =
                user.copy(
                    nickname = "변경된닉네임",
                    status = UserStatus.BANNED,
                )
            userRepository.insertOrUpdate(updatedUser)

            // then
            val foundUser = userRepository.findByEmail("update@example.com")
            assertThat(foundUser?.nickname).isEqualTo("변경된닉네임")
            assertThat(foundUser?.status).isEqualTo(UserStatus.BANNED)
        }

        @Test
        fun `모든 필드가 올바르게 저장된다`() {
            // given
            val now = LocalDateTime.of(2024, 1, 15, 10, 30, 0)
            val user =
                User(
                    id = 6L,
                    email = UserEmail("complete@example.com", true),
                    password = "hashedPassword123",
                    nickname = "완전한사용자",
                    status = UserStatus.ACTIVE,
                    role = UserRole.ADMIN,
                    createdAt = now,
                    updatedAt = now.plusHours(1),
                )

            // when
            userRepository.insertOrUpdate(user)

            // then
            val savedUser = userRepository.findByEmail("complete@example.com")
            assertThat(savedUser).isNotNull
            assertThat(savedUser?.id).isEqualTo(6L)
            assertThat(savedUser?.email?.value).isEqualTo("complete@example.com")
            assertThat(savedUser?.email?.verified).isTrue
            assertThat(savedUser?.password).isEqualTo("hashedPassword123")
            assertThat(savedUser?.nickname).isEqualTo("완전한사용자")
            assertThat(savedUser?.status).isEqualTo(UserStatus.ACTIVE)
            assertThat(savedUser?.role).isEqualTo(UserRole.ADMIN)
            assertThat(savedUser?.createdAt).isEqualTo(now)
            assertThat(savedUser?.updatedAt).isEqualTo(now.plusHours(1))
        }
    }

    @Nested
    @DisplayName("이메일로 사용자 조회")
    inner class FindByEmailTest {
        @Test
        fun `존재하는 사용자를 조회한다`() {
            // given
            val user =
                createTestUser(
                    id = 7L,
                    email = "find@example.com",
                    nickname = "찾을사용자",
                )
            userRepository.insertOrUpdate(user)

            // when
            val foundUser = userRepository.findByEmail("find@example.com")

            // then
            assertThat(foundUser).isNotNull
            assertThat(foundUser?.id).isEqualTo(7L)
            assertThat(foundUser?.getEmailValue()).isEqualTo("find@example.com")
            assertThat(foundUser?.nickname).isEqualTo("찾을사용자")
        }

        @Test
        fun `존재하지 않는 사용자는 null을 반환한다`() {
            // when
            val foundUser = userRepository.findByEmail("notfound@example.com")

            // then
            assertThat(foundUser).isNull()
        }

        @Test
        fun `이메일 검증 상태가 올바르게 매핑된다`() {
            // given
            val unverifiedUser =
                createTestUser(
                    id = 8L,
                    email = "unverified@example.com",
                    nickname = "미인증",
                    emailVerified = false,
                )
            val verifiedUser =
                createTestUser(
                    id = 9L,
                    email = "verified@example.com",
                    nickname = "인증됨",
                    emailVerified = true,
                )

            userRepository.insertOrUpdate(unverifiedUser)
            userRepository.insertOrUpdate(verifiedUser)

            // when
            val foundUnverified = userRepository.findByEmail("unverified@example.com")
            val foundVerified = userRepository.findByEmail("verified@example.com")

            // then
            assertThat(foundUnverified?.email?.verified).isFalse
            assertThat(foundVerified?.email?.verified).isTrue
        }
    }

    @Nested
    @DisplayName("도메인 모델과 엔티티 변환")
    inner class DomainEntityConversionTest {
        @Test
        fun `모든 UserStatus가 올바르게 변환된다`() {
            // given
            val statuses =
                listOf(
                    UserStatus.ACTIVE,
                    UserStatus.PENDING,
                    UserStatus.BANNED,
                    UserStatus.DORMANT,
                    UserStatus.DELETED,
                )

            statuses.forEachIndexed { index, status ->
                val user =
                    createTestUser(
                        id = (100 + index).toLong(),
                        email = "status$index@example.com",
                        nickname = "상태테스트$index",
                        status = status,
                    )

                // when
                userRepository.insertOrUpdate(user)

                // then
                val foundUser = userRepository.findByEmail("status$index@example.com")
                assertThat(foundUser?.status).isEqualTo(status)
            }
        }

        @Test
        fun `모든 UserRole이 올바르게 변환된다`() {
            // given
            val roles = listOf(UserRole.USER, UserRole.ADMIN)

            roles.forEachIndexed { index, role ->
                val user =
                    createTestUser(
                        id = (200 + index).toLong(),
                        email = "role$index@example.com",
                        nickname = "역할테스트$index",
                        role = role,
                    )

                // when
                userRepository.insertOrUpdate(user)

                // then
                val foundUser = userRepository.findByEmail("role$index@example.com")
                assertThat(foundUser?.role).isEqualTo(role)
            }
        }
    }

    @Nested
    @DisplayName("트랜잭션 처리")
    inner class TransactionTest {
        @Test
        fun `트랜잭션 롤백 시 변경사항이 저장되지 않는다`() {
            // given
            val user =
                createTestUser(
                    id = 300L,
                    email = "rollback@example.com",
                    nickname = "롤백테스트",
                )

            // when
            userRepository.insertOrUpdate(user)

            // then
            val count = jpaRepository.count()
            // 각 테스트는 독립적으로 실행되므로 이전 테스트의 영향을 받지 않음
            assertThat(count).isEqualTo(1)
        }
    }
}

// 테스트용 User 생성 헬퍼 함수
private fun createTestUser(
    id: Long,
    email: String,
    nickname: String,
    password: String = "password123",
    emailVerified: Boolean = false,
    status: UserStatus = UserStatus.ACTIVE,
    role: UserRole = UserRole.USER,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
): User {
    return User(
        id = id,
        email = UserEmail(email, emailVerified),
        password = password,
        nickname = nickname,
        status = status,
        role = role,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
