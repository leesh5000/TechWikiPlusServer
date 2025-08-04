package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository

class UserReader(
    private val repository: UserRepository,
) {
    fun getBy(email: Email): User {
        return repository.findBy(email)
            ?: throw UserNotFoundException("User with email ${email.value} not found")
    }

    fun getPendingUserBy(email: Email): User {
        return repository.findBy(email, UserStatus.PENDING)
            ?: throw UserNotFoundException("User with email ${email.value} and status ${UserStatus.PENDING} not found")
    }

    fun getActiveUserBy(email: Email): User {
        return repository.findBy(email, UserStatus.ACTIVE)
            ?: throw UserNotFoundException("User with email ${email.value} and status ${UserStatus.ACTIVE} not found")
    }
}
