package me.helloc.techwikiplus.user.infrastructure.persistence

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository
import me.helloc.techwikiplus.user.infrastructure.exception.DataAccessException
import me.helloc.techwikiplus.user.infrastructure.persistence.jpa.UserEntity
import me.helloc.techwikiplus.user.infrastructure.persistence.jpa.UserJpaRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    override fun existsByEmail(email: String): Boolean {
        return try {
            jpaRepository.existsByEmail(email)
        } catch (e: Exception) {
            throw DataAccessException("checking email existence", e)
        }
    }

    override fun existsByNickname(nickname: String): Boolean {
        return try {
            jpaRepository.existsByNickname(nickname)
        } catch (e: Exception) {
            throw DataAccessException("checking nickname existence", e)
        }
    }

    override fun insertOrUpdate(user: User) {
        try {
            jpaRepository.save(UserEntity.from(user))
        } catch (e: Exception) {
            throw DataAccessException("saving user", e)
        }
    }

    override fun findByEmail(email: String): User? {
        return try {
            jpaRepository.findByEmail(email)?.toDomain()
        } catch (e: Exception) {
            throw DataAccessException("finding user by email", e)
        }
    }
}
