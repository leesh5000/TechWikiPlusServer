package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId

interface UserRepository {
    fun findBy(userId: UserId): User?

    fun findBy(email: Email): User?

    fun exists(email: Email): Boolean

    fun exists(nickname: Nickname): Boolean

    fun save(user: User): User
}
