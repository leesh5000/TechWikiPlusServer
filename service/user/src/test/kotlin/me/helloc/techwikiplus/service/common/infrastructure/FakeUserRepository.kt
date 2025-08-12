package me.helloc.techwikiplus.service.common.infrastructure

import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository

open class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<UserId, User>()

    override fun findBy(userId: UserId): User? {
        return users[userId]
    }

    override fun findBy(email: Email): User? {
        return users.values.find { it.email == email }
    }

    override fun exists(email: Email): Boolean {
        return users.values.any { it.email == email }
    }

    override fun exists(nickname: Nickname): Boolean {
        return users.values.any { it.nickname == nickname }
    }

    override fun save(user: User): User {
        users[user.id] = user
        return user
    }

    fun clear() {
        users.clear()
    }

    fun getAll(): List<User> {
        return users.values.toList()
    }
}
