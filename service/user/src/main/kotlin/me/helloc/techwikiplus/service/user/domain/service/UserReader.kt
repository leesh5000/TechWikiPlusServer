package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository

class UserReader(
    private val repository: UserRepository,
) {
    fun findByEmail(email: Email): User {
        return repository.findBy(email)
            ?: throw UserNotFoundException("User with email ${email.value} not found")
    }
}
