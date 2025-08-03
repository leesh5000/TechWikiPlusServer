package me.helloc.techwikiplus.service.user.infrastructure.persistence

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.UserJpaRepository
import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.mapper.UserEntityMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
    private val mapper: UserEntityMapper,
) : UserRepository {
    override fun findBy(email: Email): User? {
        return jpaRepository.findByEmail(email.value)?.let {
            mapper.toDomain(it)
        }
    }

    override fun exists(email: Email): Boolean {
        return jpaRepository.existsByEmail(email.value)
    }

    @Transactional
    override fun save(user: User): User {
        val entity = mapper.toEntity(user)
        val savedEntity = jpaRepository.save(entity)
        return mapper.toDomain(savedEntity)
    }
}
