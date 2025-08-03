package me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa

import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserEntity, String> {
    fun findByEmail(email: String): UserEntity?

    fun existsByEmail(email: String): Boolean
}
