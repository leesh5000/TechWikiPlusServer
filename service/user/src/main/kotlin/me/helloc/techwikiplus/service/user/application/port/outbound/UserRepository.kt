package me.helloc.techwikiplus.service.user.application.port.outbound

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname

interface UserRepository {
    fun findBy(id: String): User?

    fun findBy(email: Email): User?

    fun findBy(
        email: Email,
        status: UserStatus,
    ): User?

    fun exists(email: Email): Boolean

    fun exists(nickname: Nickname): Boolean

    fun save(user: User): User
    fun insert(user: User): User
}
