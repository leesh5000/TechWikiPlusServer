package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.CustomException.AuthenticationException.PendingUserNotFound
import me.helloc.techwikiplus.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
open class PendingUserService(private val repository: UserRepository) {
    
    fun existOrThrows(email: String) {
        repository.findByEmail(email)
            ?.takeIf { it.isPending() }
            ?: throw PendingUserNotFound(email)
    }
}
