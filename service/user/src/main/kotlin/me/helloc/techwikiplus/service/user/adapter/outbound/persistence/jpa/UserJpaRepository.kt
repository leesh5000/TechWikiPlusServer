package me.helloc.techwikiplus.service.user.adapter.outbound.persistence.jpa

import me.helloc.techwikiplus.service.user.adapter.outbound.persistence.jpa.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, String> {
    fun existsByEmail(email: String): Boolean

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE LOWER(u.nickname) = LOWER(:nickname)")
    fun existsByNickname(nickname: String): Boolean

    @Query(
        value = """
            SELECT id, email, nickname, password, status, role, created_at, modified_at
            FROM users u
            WHERE u.email = :email
        """,
        nativeQuery = true,
    )
    fun findByEmail(email: String): UserEntity?

    @Query(
        value = """
            SELECT id, email, nickname, password, status, role, created_at, modified_at
            FROM users u
            WHERE u.email = :email AND u.status = :status
            """,
        nativeQuery = true,
    )
    fun findByEmailAndStatus(
        email: String,
        status: String,
    ): UserEntity?
}
