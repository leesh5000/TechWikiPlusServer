package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.ActiveUserNotFoundException
import me.helloc.techwikiplus.service.user.domain.exception.PendingUserNotFoundException
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
            ?: throw ActiveUserNotFoundException(userId)
    }

    @Throws(PendingUserNotFoundException::class)
    fun getPendingUserBy(email: Email): User {
        return repository.findBy(email, UserStatus.PENDING)
            ?: throw PendingUserNotFoundException(email)
    }

    @Throws(ActiveUserNotFoundException::class)
    fun getActiveUserBy(email: Email): User {
        return repository.findBy(email, UserStatus.ACTIVE)
            ?: throw ActiveUserNotFoundException(email)
    }
}
