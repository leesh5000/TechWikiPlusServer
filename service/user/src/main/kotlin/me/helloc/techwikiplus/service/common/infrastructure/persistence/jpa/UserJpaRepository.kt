package me.helloc.techwikiplus.service.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun existsByEmail(email: String): Boolean

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.nickname = :nickname")
    fun existsByNicknameIgnoreCase(
        @Param("nickname") nickname: String,
    ): Boolean

    fun findByEmail(email: String): UserEntity?
}
