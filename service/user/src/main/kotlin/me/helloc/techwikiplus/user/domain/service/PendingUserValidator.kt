package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.CustomException.AuthenticationException.PendingUserNotFound
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository

class PendingUserValidator(private val repository: UserRepository) {
    fun existsOrThrows(email: String) {
        repository.findByEmail(email)
            ?.takeIf { it.isPending() }
            ?: throw PendingUserNotFound(email)
    }
}
