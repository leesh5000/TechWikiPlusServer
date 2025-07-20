package me.helloc.techwikiplus.user.domain.repository

interface UserRepository {
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean
}
