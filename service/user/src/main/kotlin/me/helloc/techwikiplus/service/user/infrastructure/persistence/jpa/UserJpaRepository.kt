package me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa

import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, String> {
    fun existsByEmail(email: String): Boolean

    // Spring Data JPA가 자동으로 쿼리 생성 (collation 설정도 자동 적용됨)
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
}
