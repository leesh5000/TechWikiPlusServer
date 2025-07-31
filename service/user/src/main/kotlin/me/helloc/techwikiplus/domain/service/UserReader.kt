package me.helloc.techwikiplus.domain.service

import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.result.DomainResult
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.service.port.UserRepository

class UserReader(
    private val userRepository: UserRepository,
) {
    fun findByEmail(email: Email): User? {
        return userRepository.findByEmail(email)
    }
    
    fun findByEmailWithResult(email: Email): DomainResult<User> {
        return try {
            val user = userRepository.findByEmail(email)
            if (user != null) {
                DomainResult.Success(user)
            } else {
                DomainResult.Failure.NotFound("User", email.value)
            }
        } catch (e: Exception) {
            DomainResult.Failure.SystemError(e.message ?: "Unknown error occurred")
        }
    }
}