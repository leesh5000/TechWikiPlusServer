package me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa

import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, String> {
    fun existsByEmail(email: String): Boolean

    fun existsByNickname(nickname: String): Boolean

    fun findByEmail(email: String): UserEntity?

    @Query(
        "SELECT u FROM UserEntity u WHERE u.email = :email AND u.status = :status",
    )
    fun findByEmailAndStatus(
        email: String,
        status: String,
    ): UserEntity?
}
