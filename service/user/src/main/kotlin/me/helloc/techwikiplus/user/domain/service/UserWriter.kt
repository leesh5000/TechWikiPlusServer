package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
open class UserWriter(
    private val repository: UserRepository,
) {
    fun insertOrUpdate(user: User) {
        repository.insertOrUpdate(user)
    }
}
