package me.helloc.techwikiplus.service.user.infrastructure.persistence

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository

class UserRepositoryImpl : UserRepository {
    override fun findBy(email: Email): User? {
        TODO("Not yet implemented")
    }

    override fun exists(email: Email): Boolean {
        TODO("Not yet implemented")
    }

    override fun save(user: User): User {
        TODO("Not yet implemented")
    }
}
