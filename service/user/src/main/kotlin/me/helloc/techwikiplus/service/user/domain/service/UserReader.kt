package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import org.springframework.stereotype.Service

@Service
class UserReader(
    private val repository: UserRepository,
) {
    fun get(userId: UserId): User {
        val user: User =
            repository.findBy(userId)
                ?: throw UserDomainException(UserErrorCode.USER_NOT_FOUND, arrayOf(userId.value))
        user.validateUserStatus()
        return user
    }

    fun getPendingUser(email: Email): User {
        val user: User =
            repository.findBy(email)
                ?: throw UserDomainException(UserErrorCode.USER_NOT_FOUND, arrayOf(email.value))
        if (user.status != UserStatus.PENDING) {
            throw UserDomainException(UserErrorCode.NOT_FOUND_PENDING_USER, arrayOf(email.value))
        }
        return user
    }

    fun get(email: Email): User {
        val user: User =
            repository.findBy(email)
                ?: throw UserDomainException(UserErrorCode.USER_NOT_FOUND, arrayOf(email.value))
        user.validateUserStatus()
        return user
    }
}
