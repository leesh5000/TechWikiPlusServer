package me.helloc.techwikiplus.infrastructure.persistence

import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.service.port.UserRepository

class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()
    private var simulatedError: String? = null
    
    fun save(user: User) {
        users[user.email.value] = user
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