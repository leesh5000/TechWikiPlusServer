package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import org.springframework.stereotype.Service

@Service
class UserReader(
    private val repository: UserRepository,
) {
    fun get(userId: UserId): User {
        val user: User =
            repository.findBy(userId)
                ?: throw DomainException(ErrorCode.USER_NOT_FOUND, arrayOf(userId.value))
        return validateUserStatus(user)
    }

    fun getPendingUser(email: Email): User {
        val user: User =
            repository.findBy(email)
                ?: throw DomainException(ErrorCode.USER_NOT_FOUND, arrayOf(email.value))
        if (user.status != UserStatus.PENDING) {
            throw DomainException(ErrorCode.NOT_FOUND_PENDING_USER, arrayOf(email.value))
        }
        return user
    }

    fun get(email: Email): User {
        val user: User =
            repository.findBy(email)
                ?: throw DomainException(ErrorCode.USER_NOT_FOUND, arrayOf(email.value))
        return validateUserStatus(user)
    }

    private fun validateUserStatus(user: User): User {
        return when (user.status) {
            UserStatus.ACTIVE -> user
            UserStatus.DORMANT -> throw DomainException(ErrorCode.USER_DORMANT)
            UserStatus.DELETED -> throw DomainException(ErrorCode.USER_DELETED)
            UserStatus.BANNED -> throw DomainException(ErrorCode.USER_BANNED)
            UserStatus.PENDING -> throw DomainException(ErrorCode.USER_PENDING)
        }
    }
}
