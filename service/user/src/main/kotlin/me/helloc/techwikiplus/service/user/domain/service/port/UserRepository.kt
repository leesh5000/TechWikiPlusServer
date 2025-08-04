package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname

interface UserRepository {
    fun findBy(email: Email): User?

    fun exists(email: Email): Boolean

    fun exists(nickname: Nickname): Boolean

    fun save(user: User): User
}
