package me.helloc.techwikiplus.user.infrastructure.persistence

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.service.UserRepository
import me.helloc.techwikiplus.user.infrastructure.persistence.jpa.UserEntity
import me.helloc.techwikiplus.user.infrastructure.persistence.jpa.UserJpaRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository
): UserRepository {
    override fun existsByEmail(email: String): Boolean {
        return jpaRepository.existsByEmail(email)
    }

    override fun existsByNickname(nickname: String): Boolean {
        return jpaRepository.existsByNickname(nickname)
    }

    override fun insertOrUpdate(user: User) {
        jpaRepository.save(UserEntity.from(user))
    }

    override fun findByEmail(email: String): User? {
        return jpaRepository.findByEmail(email)?.toDomain()
    }
}
