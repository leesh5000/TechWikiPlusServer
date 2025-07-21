package me.helloc.techwikiplus.user.infrastructure.persistence.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import java.time.LocalDateTime

@Entity
@Table(name = "user")
class UserEntity(
    @Id
    @Column(name = "id", length = 20, nullable = false)
    val id: Long,

    @Column(name = "email", length = 255, unique = true, nullable = false)
    val email: String,

    @Column(name = "email_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    val emailVerified: Boolean,

    @Column(name = "password", length = 255, nullable = false)
    val password: String,

    @Column(name = "nickname", length = 20, unique = true, nullable = false)
    val nickname: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime
) {
    fun toDomain(): User {
        return User::class.java.getDeclaredConstructor(
            String::class.java,
            UserEmail::class.java,
            String::class.java,
            String::class.java,
            LocalDateTime::class.java,
            LocalDateTime::class.java
        ).apply {
            isAccessible = true
        }.newInstance(
            id,
            UserEmail(email, emailVerified),
            password,
            nickname,
            createdAt,
            updatedAt
        )
    }

    companion object {
        fun from(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                email = user.email.value,
                emailVerified = user.email.verified,
                password = user.password,
                nickname = user.nickname,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
