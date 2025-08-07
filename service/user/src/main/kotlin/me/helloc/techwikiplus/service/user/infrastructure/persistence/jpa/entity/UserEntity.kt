package me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_email_status", columnList = "email,status"),
        // nickname은 UNIQUE 제약조건으로 자동 인덱스 생성됨
    ],
)
class UserEntity(
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "CHAR(36)")
    val id: String,
    @Column(name = "email", nullable = false, unique = true, length = 255)
    val email: String,
    // 저장은 그대로, 조회 시에는 대소문자 구분 X (utf8mb4_0900_ai_ci: case insensitive)
    @Column(
        name = "nickname",
        nullable = false,
        unique = true,
        length = 50,
        columnDefinition = "VARCHAR(50) COLLATE utf8mb4_0900_ai_ci",
    )
    val nickname: String,
    @Column(name = "password", nullable = false, length = 255)
    val password: String,
    @Column(name = "status", nullable = false, length = 20)
    val status: String = "PENDING",
    @Column(name = "role", nullable = false, length = 20)
    val role: String = "USER",
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "modified_at", nullable = false)
    val modifiedAt: Instant,
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        id = "",
        email = "",
        nickname = "",
        password = "",
        status = "PENDING",
        role = "USER",
        createdAt = Instant.now(),
        modifiedAt = Instant.now(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserEntity) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "UserEntity(id='$id', email='$email', nickname='$nickname', " +
            "status='$status', role='$role', createdAt=$createdAt, modifiedAt=$modifiedAt)"
    }
}
