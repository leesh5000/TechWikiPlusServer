package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User

interface UserRepository {
    fun existsByEmail(email: String): Boolean

    fun existsByNickname(nickname: String): Boolean

    fun insertOrUpdate(user: User)

    fun findByEmail(email: String): User?
}
