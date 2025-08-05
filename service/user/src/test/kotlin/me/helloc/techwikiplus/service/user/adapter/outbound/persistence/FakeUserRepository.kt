package me.helloc.techwikiplus.service.user.adapter.outbound.persistence

import me.helloc.techwikiplus.service.user.application.port.outbound.UserRepository
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname

class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()
    private var simulatedError: String? = null

    override fun findBy(id: String): User? {
        simulatedError?.let { throw RuntimeException(it) }
        return users.values.find { it.id == id }
    }

    override fun findBy(email: Email): User? {
        simulatedError?.let { throw RuntimeException(it) }
        return users[email.value]
    }

    override fun findBy(
        email: Email,
        status: UserStatus,
    ): User? {
        simulatedError?.let { throw RuntimeException(it) }
        return users[email.value]?.takeIf { it.status == status }
    }

    override fun exists(email: Email): Boolean {
        simulatedError?.let { throw RuntimeException(it) }
        return users.containsKey(email.value)
    }

    override fun exists(nickname: Nickname): Boolean {
        simulatedError?.let { throw RuntimeException(it) }
        return users.values.any { it.nickname.value.lowercase() == nickname.value.lowercase() }
    }

    override fun save(user: User): User {
        simulatedError?.let { throw RuntimeException(it) }
        // Save or update
        users[user.email.value] = user
        return user
    }

    fun clear() {
        users.clear()
        simulatedError = null
    }
}
