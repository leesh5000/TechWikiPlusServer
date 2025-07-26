package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository

class UserWriter(
    private val repository: UserRepository,
) {
    fun insertOrUpdate(user: User) {
        repository.insertOrUpdate(user)
    }
}
