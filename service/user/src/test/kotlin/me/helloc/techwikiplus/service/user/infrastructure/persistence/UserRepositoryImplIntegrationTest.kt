package me.helloc.techwikiplus.service.user.infrastructure.persistence

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@DataJpaTest
@ActiveProfiles("test")
@Import(UserEntityMapper::class)
class UserRepositoryImplIntegrationTest : FunSpec() {
    @Autowired
    private lateinit var jpaRepository: UserJpaRepository

    @Autowired
    private lateinit var mapper: UserEntityMapper

    init {
        extensions(SpringExtension)

        beforeTest {
            jpaRepository.deleteAll()
        }

        test("save 메서드는 User를 저장하고 저장된 User를 반환해야 한다") {
            // Given
            val userRepository = UserRepositoryImpl(jpaRepository, mapper)
            val user =
                User(
                    id = "123456789",
                    email = Email("user@example.com"),
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password_hash"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now(),
                )

            // When
            val savedUser = userRepository.save(user)

            // Then
            savedUser shouldNotBe null
            savedUser.id shouldBe user.id
            savedUser.email shouldBe user.email
            savedUser.nickname shouldBe user.nickname
            savedUser.encodedPassword shouldBe user.encodedPassword
            savedUser.status shouldBe user.status
            savedUser.role shouldBe user.role
        }

        test("findBy 메서드는 이메일로 User를 찾아 반환해야 한다") {
            // Given
            val userRepository = UserRepositoryImpl(jpaRepository, mapper)
            val user =
                User(
                    id = "123456789",
                    email = Email("user@example.com"),
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password_hash"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now(),
                )
            userRepository.save(user)

            // When
            val foundUser = userRepository.findBy(Email("user@example.com"))

            // Then
            foundUser shouldNotBe null
            foundUser?.id shouldBe user.id
            foundUser?.email shouldBe user.email
            foundUser?.nickname shouldBe user.nickname
        }

        test("findBy 메서드는 존재하지 않는 이메일에 대해 null을 반환해야 한다") {
            // Given
            val userRepository = UserRepositoryImpl(jpaRepository, mapper)
            val user =
                User(
                    id = "123456789",
                    email = Email("user@example.com"),
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password_hash"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now(),
                )
            userRepository.save(user)

            // When
            val foundUser = userRepository.findBy(Email("nonexistent@example.com"))

            // Then
            foundUser shouldBe null
        }

        test("exists 메서드는 이메일이 존재하면 true를 반환해야 한다") {
            // Given
            val userRepository = UserRepositoryImpl(jpaRepository, mapper)
            val user =
                User(
                    id = "123456789",
                    email = Email("user@example.com"),
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password_hash"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now(),
                )
            userRepository.save(user)

            // When
            val exists = userRepository.exists(Email("user@example.com"))

            // Then
            exists shouldBe true
        }

        test("exists 메서드는 이메일이 존재하지 않으면 false를 반환해야 한다") {
            // Given
            val userRepository = UserRepositoryImpl(jpaRepository, mapper)

            // When
            val exists = userRepository.exists(Email("nonexistent@example.com"))

            // Then
            exists shouldBe false
        }

        test("동일한 ID로 User를 저장하면 업데이트되어야 한다") {
            // Given
            val userRepository = UserRepositoryImpl(jpaRepository, mapper)
            val originalUser =
                User(
                    id = "123456789",
                    email = Email("user@example.com"),
                    nickname = Nickname("testuser"),
                    encodedPassword = EncodedPassword("encoded_password_hash"),
                    status = UserStatus.ACTIVE,
                    role = UserRole.USER,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now(),
                )
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

            // Verify only one user exists
            jpaRepository.count() shouldBe 1
        }

        test("다양한 UserStatus와 UserRole을 올바르게 저장하고 조회해야 한다") {
            // Given
            val userRepository = UserRepositoryImpl(jpaRepository, mapper)
            val users =
                listOf(
                    createTestUser("1", "user1@example.com", "user1", UserStatus.ACTIVE, UserRole.USER),
                    createTestUser("2", "user2@example.com", "user2", UserStatus.DORMANT, UserRole.ADMIN),
                    createTestUser("3", "user3@example.com", "user3", UserStatus.BANNED, UserRole.USER),
                    createTestUser("4", "user4@example.com", "user4", UserStatus.PENDING, UserRole.USER),
                    createTestUser("5", "user5@example.com", "user5", UserStatus.DELETED, UserRole.ADMIN),
                )

            // When
            users.forEach { userRepository.save(it) }

            // Then
            users.forEach { user ->
                val foundUser = userRepository.findBy(user.email)
                foundUser shouldNotBe null
                foundUser?.status shouldBe user.status
                foundUser?.role shouldBe user.role
            }
        }
    }
}

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
