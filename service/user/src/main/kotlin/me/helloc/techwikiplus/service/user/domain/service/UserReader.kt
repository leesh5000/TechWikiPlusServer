package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.UserRepository
import org.springframework.stereotype.Service

@Service
class UserReader(
    private val repository: UserRepository,
) {
    fun getActiveUserBy(userId: UserId): User {
        return repository.findBy(userId)
            ?: throw DomainException(ErrorCode.USER_NOT_FOUND, arrayOf(userId.value))
    }

    @Throws(DomainException::class)
    fun getPendingUserBy(email: Email): User {
        return repository.findBy(email, UserStatus.PENDING)
            ?: throw DomainException(ErrorCode.PENDING_USER_NOT_FOUND, arrayOf(email.value))
    }

    @Throws(DomainException::class)
    fun getActiveUserBy(email: Email): User {
        return repository.findBy(email, UserStatus.ACTIVE)
            ?: throw DomainException(ErrorCode.USER_NOT_FOUND, arrayOf(email.value))
    }
}
