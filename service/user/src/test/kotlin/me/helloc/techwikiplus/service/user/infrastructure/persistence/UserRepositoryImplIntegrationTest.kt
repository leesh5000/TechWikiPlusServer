package me.helloc.techwikiplus.service.user.infrastructure.persistence

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.UserJpaRepository
import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.mapper.UserEntityMapper
import me.helloc.techwikiplus.service.user.test.config.MySQLTestContainerBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * UserRepositoryImpl의 MySQL 통합 테스트
 *
 * 이 테스트는 실제 MySQL 데이터베이스를 사용하여
 * UserRepository의 모든 데이터베이스 연산을 검증합니다.
 */
@Import(UserEntityMapper::class)
@Transactional
class UserRepositoryImplIntegrationTest : MySQLTestContainerBase() {
    @Autowired
    private lateinit var jpaRepository: UserJpaRepository

    @Autowired
    private lateinit var mapper: UserEntityMapper

    private lateinit var userRepository: UserRepositoryImpl

    init {
        beforeEach {
            userRepository = UserRepositoryImpl(jpaRepository, mapper)
            jpaRepository.deleteAll()
            jpaRepository.flush()
        }

        context("save 메서드") {
            test("User를 저장하고 저장된 User를 반환해야 한다") {
                // Given
                val user = createDefaultUser()

                // When
                val savedUser = userRepository.save(user)

                // Then
                verifyUserEquality(savedUser, user)
            }

            test("동일한 ID로 User를 저장하면 업데이트되어야 한다") {
                // Given
                val originalUser = createDefaultUser()
                userRepository.save(originalUser)

                // When
                val updatedUser =
                    originalUser.copy(
                        nickname = Nickname("updateduser"),
                        status = UserStatus.DORMANT,
                    )
                val savedUser = userRepository.save(updatedUser)

                // Then
                savedUser.nickname.value shouldBe "updateduser"
                savedUser.status shouldBe UserStatus.DORMANT
                jpaRepository.count() shouldBe 1
            }
        }

        context("findBy 메서드") {
            test("이메일로 User를 찾아 반환해야 한다") {
                // Given
                val user = createDefaultUser()
                userRepository.save(user)

                // When
                val foundUser = userRepository.findBy(user.email)

                // Then
                foundUser shouldNotBe null
                foundUser?.let { verifyUserEquality(it, user) }
            }

            test("존재하지 않는 이메일에 대해 null을 반환해야 한다") {
                // Given
                val user = createDefaultUser()
                userRepository.save(user)

                // When
                val foundUser = userRepository.findBy(Email("nonexistent@example.com"))

                // Then
                foundUser shouldBe null
            }
        }

        context("exists 메서드") {
            test("이메일이 존재하면 true를 반환해야 한다") {
                // Given
                val user = createDefaultUser()
                userRepository.save(user)

                // When
                val exists = userRepository.exists(user.email)

                // Then
                exists shouldBe true
            }

            test("이메일이 존재하지 않으면 false를 반환해야 한다") {
                // Given - 빈 데이터베이스

                // When
                val exists = userRepository.exists(Email("nonexistent@example.com"))

                // Then
                exists shouldBe false
            }
        }

        context("다양한 User 상태 처리") {
            test("다양한 UserStatus와 UserRole을 올바르게 저장하고 조회해야 한다") {
                // Given
                val testScenarios =
                    listOf(
                        UserTestScenario("1", "user1@example.com", "user1", UserStatus.ACTIVE, UserRole.USER),
                        UserTestScenario("2", "user2@example.com", "user2", UserStatus.DORMANT, UserRole.ADMIN),
                        UserTestScenario("3", "user3@example.com", "user3", UserStatus.BANNED, UserRole.USER),
                        UserTestScenario("4", "user4@example.com", "user4", UserStatus.PENDING, UserRole.USER),
                        UserTestScenario("5", "user5@example.com", "user5", UserStatus.DELETED, UserRole.ADMIN),
                    )

                // When
                val savedUsers =
                    testScenarios.map { scenario ->
                        userRepository.save(
                            createTestUser(
                                id = scenario.id,
                                email = scenario.email,
                                nickname = scenario.nickname,
                                status = scenario.status,
                                role = scenario.role,
                            ),
                        )
                    }

                // Then
                savedUsers.forEach { savedUser ->
                    val foundUser = userRepository.findBy(savedUser.email)
                    foundUser shouldNotBe null
                    foundUser?.status shouldBe savedUser.status
                    foundUser?.role shouldBe savedUser.role
                }
            }
        }
    }

    /**
     * 기본 테스트 User 생성
     */
    private fun createDefaultUser() =
        createTestUser(
            id = "123456789",
            email = "user@example.com",
            nickname = "testuser",
        )

    /**
     * User 동등성 검증
     */
    private fun verifyUserEquality(
        actual: User,
        expected: User,
    ) {
        actual.id shouldBe expected.id
        actual.email shouldBe expected.email
        actual.nickname shouldBe expected.nickname
        actual.encodedPassword shouldBe expected.encodedPassword
        actual.status shouldBe expected.status
        actual.role shouldBe expected.role
    }
}

/**
 * 테스트 시나리오 데이터 클래스
 */
private data class UserTestScenario(
    val id: String,
    val email: String,
    val nickname: String,
    val status: UserStatus,
    val role: UserRole,
)

/**
 * 테스트 User 생성 헬퍼 함수
 */
private fun createTestUser(
    id: String,
    email: String,
    nickname: String,
    status: UserStatus = UserStatus.ACTIVE,
    role: UserRole = UserRole.USER,
) = User(
    id = id,
    email = Email(email),
    nickname = Nickname(nickname),
    encodedPassword = EncodedPassword("encoded_password_hash"),
    status = status,
    role = role,
    createdAt = Instant.now(),
    modifiedAt = Instant.now(),
)
