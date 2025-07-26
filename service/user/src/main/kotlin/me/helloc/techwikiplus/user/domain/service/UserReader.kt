package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.exception.notfound.UserEmailNotFoundException
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository

class UserReader(
    private val repository: UserRepository,
) {
    fun readByEmailOrThrows(email: String): User {
        return repository.findByEmail(email)
            ?: throw UserEmailNotFoundException(email)
    }
}
