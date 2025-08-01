package me.helloc.techwikiplus.service.user.infrastructure.persistence

import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.service.port.UserRepository

class UserRepositoryImpl: UserRepository {
    override fun findByEmail(email: Email): User? {
        TODO("Not yet implemented")
    }
}
