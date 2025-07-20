package me.helloc.techwikiplus.user.domain

import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidNickname
import java.time.LocalDateTime

class User(
    val id: String,
    val email: UserEmail,
    val password: String,
    val nickname: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        private val NICKNAME_REGEX = "^[a-zA-Z0-9가-힣]{2,20}$".toRegex()

        private fun validateNickname(nickname: String) {
            if (!NICKNAME_REGEX.matches(nickname)) {
                throw InvalidNickname(nickname)
            }
        }
    }

    init {
        validateNickname(nickname)
    }

    fun copy(
        id: String = this.id,
        nickname: String = this.nickname,
        email: UserEmail = this.email,
        password: String = this.password,
        createdAt: LocalDateTime = this.createdAt,
        updatedAt: LocalDateTime = LocalDateTime.now(),
    ): User {
        return User(
            id = id,
            nickname = nickname,
            email = email,
            password = password,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun changeNickname(newNickname: String): User {
        validateNickname(newNickname)
        return copy(
            nickname = newNickname,
        )
    }

    fun verifyEmail(): User {
        return copy(
            email = this.email.verify(),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
