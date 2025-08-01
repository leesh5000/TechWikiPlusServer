package me.helloc.techwikiplus.service.user.infrastructure.persistence

import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.type.UserRole
import me.helloc.techwikiplus.domain.model.type.UserStatus
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.model.value.Nickname
import me.helloc.techwikiplus.domain.model.value.Password
import me.helloc.techwikiplus.domain.service.port.UserRepository
import java.time.Instant
import java.util.UUID

class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()
    private var simulatedError: String? = null

    fun save(user: User) {
        users[user.email.value] = user
    }

    fun save(
        email: String,
        password: String,
        name: String,
        status: UserStatus = UserStatus.PENDING,
    ): User {
        val user =
            User(
                id = UUID.randomUUID().toString(),
                email = Email(email),
                password = Password(password),
                nickname = Nickname(name),
                role = UserRole.USER,
                status = status,
                createdAt = Instant.now(),
                modifiedAt = Instant.now(),
            )
        users[email] = user
        return user
    }

    fun findByEmail(email: String): User? {
        return users[email]
    }

    fun clear() {
        users.clear()
    }

    fun simulateError(errorMessage: String) {
        simulatedError = errorMessage
    }

    fun clearError() {
        simulatedError = null
    }

    override fun findByEmail(email: Email): User? {
        simulatedError?.let { throw RuntimeException(it) }
        return users[email.value]
    }
}
