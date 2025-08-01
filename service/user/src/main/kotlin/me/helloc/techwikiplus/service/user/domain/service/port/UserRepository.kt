package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.value.Email

interface UserRepository {
    fun findByEmail(email: Email): User?
}
