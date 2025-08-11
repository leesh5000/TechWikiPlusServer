package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.service.port.ClockHolder
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserModifier(
    private val clockHolder: ClockHolder,
    private val repository: UserRepository,
) {
    fun activate(user: User): User {
        val modifiedAt: Instant = clockHolder.now()
        val activatedUser: User = user.activate(modifiedAt)
        return repository.save(activatedUser)
    }

    fun setPending(user: User): User {
        val modifiedAt: Instant = clockHolder.now()
        val pendingUser: User = user.setPending(modifiedAt)
        return repository.save(pendingUser)
    }
}
