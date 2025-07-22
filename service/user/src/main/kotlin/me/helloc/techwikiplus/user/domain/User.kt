package me.helloc.techwikiplus.user.domain

import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidNickname
import me.helloc.techwikiplus.user.domain.service.Clock
import java.time.LocalDateTime

class User(
    val id: Long,
    val email: UserEmail,
    val password: String,
    val nickname: String,
    val status: UserStatus = UserStatus.ACTIVE,
    val role: UserRole = UserRole.USER,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        private val NICKNAME_REGEX = "^[a-zA-Z0-9가-힣]{2,20}$".toRegex()

        private fun validateNickname(nickname: String) {
            if (!NICKNAME_REGEX.matches(nickname)) {
                throw InvalidNickname(nickname)
            }
        }

        fun withPendingUser(
            id: Long,
            email: UserEmail,
            nickname: String,
            password: String,
            clock: Clock = Clock.system,
        ): User {
            validateNickname(nickname)
            return User(
                id = id,
                email = email,
                password = password,
                nickname = nickname,
                status = UserStatus.PENDING,
                createdAt = clock.localDateTime(),
                updatedAt = clock.localDateTime(),
            )
        }
    }

    init {
        validateNickname(nickname)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun copy(
        id: Long = this.id,
        nickname: String = this.nickname,
        email: UserEmail = this.email,
        password: String = this.password,
        status: UserStatus = this.status,
        role: UserRole = this.role,
        clock: Clock = Clock.system,
    ): User {
        return User(
            id = id,
            nickname = nickname,
            email = email,
            password = password,
            status = status,
            role = role,
            createdAt = createdAt,
            updatedAt = clock.localDateTime(),
        )
    }

    fun changeNickname(
        newNickname: String,
        clock: Clock = Clock.system,
    ): User {
        validateNickname(newNickname)
        return copy(
            nickname = newNickname,
            clock = clock,
        )
    }

    fun verifyEmail(clock: Clock = Clock.system): User {
        return copy(
            email = this.email.verify(),
            clock = clock,
        )
    }

    fun isPending(): Boolean {
        return status == UserStatus.PENDING
    }

    fun completeSignUp(clock: Clock = Clock.system): User {
        val verifiedUser: User = this.verifyEmail(clock)
        return verifiedUser.copy(
            status = UserStatus.ACTIVE,
            clock = clock,
        )
    }

    fun email(): String {
        return email.value
    }
}
