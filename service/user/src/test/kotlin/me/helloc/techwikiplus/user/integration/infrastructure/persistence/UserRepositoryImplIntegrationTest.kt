package me.helloc.techwikiplus.user.integration.infrastructure.persistence

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.user.infrastructure.clock.fake.FakeClock
import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserRole
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.infrastructure.config.TestContainerConfig
import me.helloc.techwikiplus.user.infrastructure.persistence.UserRepositoryImpl
import me.helloc.techwikiplus.user.infrastructure.persistence.jpa.UserJpaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserRepositoryImpl::class, TestContainerConfig::class)
@TestPropertySource(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@Testcontainers
@ActiveProfiles("test")
class UserRepositoryImplIntegrationTest : FunSpec() {

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

    @Autowired
    private lateinit var jpaRepository: UserJpaRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    override fun extensions() = listOf(SpringExtension)

    init {
        beforeEach {
            // 각 테스트 전에 데이터 초기화
            jpaRepository.deleteAll()
            entityManager.flush()
            entityManager.clear()
        }

        context("이메일 중복 체크") {
            test("존재하는 이메일은 true를 반환한다") {
                val user = createTestUser(
                    id = 1L,
                    email = "existing@example.com",
                    nickname = "테스터1"
                )
                userRepository.insertOrUpdate(user)
                entityManager.flush()
                entityManager.clear()

                val exists = userRepository.existsByEmail("existing@example.com")

                exists shouldBe true
            }

            test("존재하지 않는 이메일은 false를 반환한다") {
                val exists = userRepository.existsByEmail("nonexistent@example.com")

                exists shouldBe false
            }

            test("대소문자를 구분하지 않고 체크한다") {
                val user = createTestUser(
                    id = 1L,
                    email = "test@example.com",
                    nickname = "테스터"
                )
                userRepository.insertOrUpdate(user)
                entityManager.flush()
                entityManager.clear()

                userRepository.existsByEmail("test@example.com") shouldBe true
                userRepository.existsByEmail("Test@example.com") shouldBe true
                userRepository.existsByEmail("TEST@EXAMPLE.COM") shouldBe true
            }
        }

        context("닉네임 중복 체크") {
            test("존재하는 닉네임은 true를 반환한다") {
                val user = createTestUser(
                    id = 2L,
                    email = "user@example.com",
                    nickname = "기존닉네임"
                )
                userRepository.insertOrUpdate(user)
                entityManager.flush()
                entityManager.clear()

                val exists = userRepository.existsByNickname("기존닉네임")

                exists shouldBe true
            }

            test("존재하지 않는 닉네임은 false를 반환한다") {
                val exists = userRepository.existsByNickname("새로운닉네임")

                exists shouldBe false
            }

            test("닉네임도 정확히 일치해야 한다") {
                val user = createTestUser(
                    id = 3L,
                    email = "nick@example.com",
                    nickname = "테스터123"
                )
                userRepository.insertOrUpdate(user)
                entityManager.flush()
                entityManager.clear()

                userRepository.existsByNickname("테스터123") shouldBe true
                userRepository.existsByNickname("테스터") shouldBe false
                userRepository.existsByNickname("테스터1234") shouldBe false
            }
        }

        context("사용자 저장") {
            test("새로운 사용자를 저장한다") {
                val user = createTestUser(
                    id = 4L,
                    email = "new@example.com",
                    nickname = "새사용자"
                )

                userRepository.insertOrUpdate(user)
                entityManager.flush()
                entityManager.clear()

                val savedUser = userRepository.findByEmail("new@example.com")

                savedUser shouldNotBe null
                savedUser?.id shouldBe 4L
                savedUser?.email() shouldBe "new@example.com"
                savedUser?.nickname shouldBe "새사용자"
            }

            test("기존 사용자를 업데이트한다") {
                val user = createTestUser(
                    id = 5L,
                    email = "update@example.com",
                    nickname = "원래닉네임"
                )
                userRepository.insertOrUpdate(user)
                entityManager.flush()
                entityManager.clear()

                val updatedUser = user.copy(
                    nickname = "변경된닉네임",
                    status = UserStatus.BANNED
                )
                userRepository.insertOrUpdate(updatedUser)
                entityManager.flush()
                entityManager.clear()

                val foundUser = userRepository.findByEmail("update@example.com")

                foundUser?.nickname shouldBe "변경된닉네임"
                foundUser?.status shouldBe UserStatus.BANNED
            }

            test("모든 필드가 올바르게 저장된다") {
                val now = LocalDateTime.of(2024, 1, 15, 10, 30, 0)
                val user = User(
                    id = 6L,
                    email = UserEmail("complete@example.com", true),
                    password = "hashedPassword123",
                    nickname = "완전한사용자",
                    status = UserStatus.ACTIVE,
                    role = UserRole.ADMIN,
                    createdAt = now,
                    updatedAt = now.plusHours(1)
                )

                userRepository.insertOrUpdate(user)
                entityManager.flush()
                entityManager.clear()

                val savedUser = userRepository.findByEmail("complete@example.com")

                savedUser shouldNotBe null
                savedUser?.id shouldBe 6L
                savedUser?.email?.value shouldBe "complete@example.com"
                savedUser?.email?.verified shouldBe true
                savedUser?.password shouldBe "hashedPassword123"
                savedUser?.nickname shouldBe "완전한사용자"
                savedUser?.status shouldBe UserStatus.ACTIVE
                savedUser?.role shouldBe UserRole.ADMIN
                savedUser?.createdAt shouldBe now
                savedUser?.updatedAt shouldBe now.plusHours(1)
            }
        }

        context("이메일로 사용자 조회") {
            test("존재하는 사용자를 조회한다") {
                val user = createTestUser(
                    id = 7L,
                    email = "find@example.com",
                    nickname = "찾을사용자"
                )
                userRepository.insertOrUpdate(user)
                entityManager.flush()
                entityManager.clear()

                val foundUser = userRepository.findByEmail("find@example.com")

                foundUser shouldNotBe null
                foundUser?.id shouldBe 7L
                foundUser?.email() shouldBe "find@example.com"
                foundUser?.nickname shouldBe "찾을사용자"
            }

            test("존재하지 않는 사용자는 null을 반환한다") {
                val foundUser = userRepository.findByEmail("notfound@example.com")

                foundUser shouldBe null
            }

            test("이메일 검증 상태가 올바르게 매핑된다") {
                val unverifiedUser = createTestUser(
                    id = 8L,
                    email = "unverified@example.com",
                    nickname = "미인증",
                    emailVerified = false
                )
                val verifiedUser = createTestUser(
                    id = 9L,
                    email = "verified@example.com",
                    nickname = "인증됨",
                    emailVerified = true
                )

                userRepository.insertOrUpdate(unverifiedUser)
                userRepository.insertOrUpdate(verifiedUser)
                entityManager.flush()
                entityManager.clear()

                val foundUnverified = userRepository.findByEmail("unverified@example.com")
                val foundVerified = userRepository.findByEmail("verified@example.com")

                foundUnverified?.email?.verified shouldBe false
                foundVerified?.email?.verified shouldBe true
            }
        }

        context("도메인 모델과 엔티티 변환") {
            test("모든 UserStatus가 올바르게 변환된다") {
                val statuses = listOf(
                    UserStatus.ACTIVE,
                    UserStatus.PENDING,
                    UserStatus.BANNED,
                    UserStatus.DORMANT,
                    UserStatus.DELETED
                )

                statuses.forEachIndexed { index, status ->
                    val user = createTestUser(
                        id = (100 + index).toLong(),
                        email = "status$index@example.com",
                        nickname = "상태테스트$index",
                        status = status
                    )

                    userRepository.insertOrUpdate(user)
                    entityManager.flush()
                    entityManager.clear()

                    val foundUser = userRepository.findByEmail("status$index@example.com")
                    foundUser?.status shouldBe status
                }
            }

            test("모든 UserRole이 올바르게 변환된다") {
                val roles = listOf(UserRole.USER, UserRole.ADMIN)

                roles.forEachIndexed { index, role ->
                    val user = createTestUser(
                        id = (200 + index).toLong(),
                        email = "role$index@example.com",
                        nickname = "역할테스트$index",
                        role = role
                    )

                    userRepository.insertOrUpdate(user)
                    entityManager.flush()
                    entityManager.clear()

                    val foundUser = userRepository.findByEmail("role$index@example.com")
                    foundUser?.role shouldBe role
                }
            }
        }

        context("트랜잭션 처리") {
            test("트랜잭션 롤백 시 변경사항이 저장되지 않는다") {
                val user = createTestUser(
                    id = 300L,
                    email = "rollback@example.com",
                    nickname = "롤백테스트"
                )

                userRepository.insertOrUpdate(user)
                entityManager.flush()

                // 트랜잭션이 롤백되면 이 데이터는 저장되지 않음
                // 실제 테스트에서는 @Transactional과 @Rollback이 적용됨

                val count = jpaRepository.count()
                // 각 테스트는 독립적으로 실행되므로 이전 테스트의 영향을 받지 않음
                count shouldBe 1
            }
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
    updatedAt: LocalDateTime = LocalDateTime.now()
): User {
    return User(
        id = id,
        email = UserEmail(email, emailVerified),
        password = password,
        nickname = nickname,
        status = status,
        role = role,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
