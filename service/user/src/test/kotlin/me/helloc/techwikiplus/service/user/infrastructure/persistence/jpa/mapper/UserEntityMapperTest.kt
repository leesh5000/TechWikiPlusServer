package me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.mapper

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.entity.UserEntity
import java.time.Instant

class UserEntityMapperTest : FunSpec({

    val mapper = UserEntityMapper()

    test("UserEntity를 User 도메인 모델로 변환해야 한다") {
        // Given
        val entity =
            UserEntity(
                id = "123456789",
                email = "user@example.com",
                nickname = "testuser",
                password = "encoded_password_hash",
                status = "ACTIVE",
                role = "USER",
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
            )

        // When
        val user = mapper.toDomain(entity)

        // Then
        user.id shouldBe entity.id
        user.email.value shouldBe entity.email
        user.nickname.value shouldBe entity.nickname
        user.encodedPassword.value shouldBe entity.password
        user.status shouldBe UserStatus.ACTIVE
        user.role shouldBe UserRole.USER
        user.createdAt shouldBe entity.createdAt
        user.modifiedAt shouldBe entity.modifiedAt
    }

    test("User 도메인 모델을 UserEntity로 변환해야 한다") {
        // Given
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
        val entity = mapper.toEntity(user)

        // Then
        entity.id shouldBe user.id
        entity.email shouldBe user.email.value
        entity.nickname shouldBe user.nickname.value
        entity.password shouldBe user.encodedPassword.value
        entity.status shouldBe "ACTIVE"
        entity.role shouldBe "USER"
        entity.createdAt shouldBe user.createdAt
        entity.modifiedAt shouldBe user.modifiedAt
    }

    test("다양한 UserStatus를 올바르게 변환해야 한다") {
        // Given
        val statuses =
            listOf(
                UserStatus.ACTIVE to "ACTIVE",
                UserStatus.DORMANT to "DORMANT",
                UserStatus.BANNED to "BANNED",
                UserStatus.PENDING to "PENDING",
                UserStatus.DELETED to "DELETED",
            )

        statuses.forEach { (domainStatus, entityStatus) ->
            // When converting from entity to domain
            val entity = createTestEntity(status = entityStatus)
            val user = mapper.toDomain(entity)

            // Then
            user.status shouldBe domainStatus

            // When converting from domain to entity
            val userWithStatus = createTestUser(status = domainStatus)
            val entityFromDomain = mapper.toEntity(userWithStatus)

            // Then
            entityFromDomain.status shouldBe entityStatus
        }
    }

    test("다양한 UserRole을 올바르게 변환해야 한다") {
        // Given
        val roles =
            listOf(
                UserRole.USER to "USER",
                UserRole.ADMIN to "ADMIN",
            )

        roles.forEach { (domainRole, entityRole) ->
            // When converting from entity to domain
            val entity = createTestEntity(role = entityRole)
            val user = mapper.toDomain(entity)

            // Then
            user.role shouldBe domainRole

            // When converting from domain to entity
            val userWithRole = createTestUser(role = domainRole)
            val entityFromDomain = mapper.toEntity(userWithRole)

            // Then
            entityFromDomain.role shouldBe entityRole
        }
    }

    test("동일한 UserEntity를 변환하면 동일한 도메인 객체를 생성해야 한다") {
        // Given
        val entity = createTestEntity()

        // When
        val user1 = mapper.toDomain(entity)
        val user2 = mapper.toDomain(entity)

        // Then
        user1 shouldBe user2
        user1.email shouldBe user2.email
        user1.nickname shouldBe user2.nickname
    }

    test("동일한 User를 변환하면 동일한 속성을 가진 엔티티 객체를 생성해야 한다") {
        // Given
        val user = createTestUser()

        // When
        val entity1 = mapper.toEntity(user)
        val entity2 = mapper.toEntity(user)

        // Then
        entity1 shouldBe entity2 // equals by id
        entity1.id shouldBe entity2.id
        entity1.email shouldBe entity2.email
        entity1.nickname shouldBe entity2.nickname
        entity1.password shouldBe entity2.password
        entity1.status shouldBe entity2.status
        entity1.role shouldBe entity2.role
        entity1.createdAt shouldBe entity2.createdAt
        entity1.modifiedAt shouldBe entity2.modifiedAt
    }
})

// Helper functions
private fun createTestEntity(
    id: String = "123456789",
    email: String = "user@example.com",
    nickname: String = "testuser",
    password: String = "encoded_password_hash",
    status: String = "ACTIVE",
    role: String = "USER",
    createdAt: Instant = Instant.now(),
    modifiedAt: Instant = createdAt,
) = UserEntity(
    id = id,
    email = email,
    nickname = nickname,
    password = password,
    status = status,
    role = role,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
)

private fun createTestUser(
    id: String = "123456789",
    email: Email = Email("user@example.com"),
    nickname: Nickname = Nickname("testuser"),
    encodedPassword: EncodedPassword = EncodedPassword("encoded_password_hash"),
    status: UserStatus = UserStatus.ACTIVE,
    role: UserRole = UserRole.USER,
    createdAt: Instant = Instant.now(),
    modifiedAt: Instant = createdAt,
) = User(
    id = id,
    email = email,
    nickname = nickname,
    encodedPassword = encodedPassword,
    status = status,
    role = role,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
)
