package me.helloc.techwikiplus.service.user.infrastructure.persistence

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository

class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()
    private var simulatedError: String? = null

    override fun findBy(email: Email): User? {
        simulatedError?.let { throw RuntimeException(it) }
        return users[email.value]
    }

    override fun exists(email: Email): Boolean {
        simulatedError?.let { throw RuntimeException(it) }
        return users.containsKey(email.value)
    }

    override fun save(user: User): User {
        simulatedError?.let { throw RuntimeException(it) }
        if (users.containsKey(user.email.value)) {
            throw RuntimeException("User with email ${user.email.value} already exists")
        }
        users[user.email.value] = user
        return user
    }

    fun clear() {
        users.clear()
        simulatedError = null
    }
}
