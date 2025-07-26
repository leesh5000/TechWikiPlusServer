package me.helloc.techwikiplus.user.infrastructure.persistence.fake

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository

class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<Long, User>()
    private val emailIndex = mutableMapOf<String, Long>()
    private val nicknameIndex = mutableMapOf<String, Long>()

    override fun insertOrUpdate(user: User) {
        // If updating existing user, remove old email and nickname from indexes
        users[user.id]?.let { oldUser ->
            emailIndex.remove(oldUser.getEmailValue())
            nicknameIndex.remove(oldUser.nickname)
        }

        users[user.id] = user
        emailIndex[user.getEmailValue()] = user.id
        nicknameIndex[user.nickname] = user.id
    }

    override fun findByEmail(email: String): User? {
        val id = emailIndex[email] ?: return null
        return users[id]
    }

    override fun existsByEmail(email: String): Boolean {
        return emailIndex.containsKey(email)
    }

    override fun existsByNickname(nickname: String): Boolean {
        return nicknameIndex.containsKey(nickname)
    }

    fun clear() {
        users.clear()
        emailIndex.clear()
        nicknameIndex.clear()
    }

    fun findAll(): List<User> = users.values.toList()
}
