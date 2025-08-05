package me.helloc.techwikiplus.service.user.domain.model

import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import java.time.Instant

class User(
    val id: String,
    val email: Email,
    val nickname: Nickname,
    val encodedPassword: EncodedPassword,
    val status: UserStatus,
    val role: UserRole,
    val createdAt: Instant,
    val modifiedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "User ID cannot be blank" }
    }

    fun copy(
        id: String = this.id,
        email: Email = this.email,
        nickname: Nickname = this.nickname,
        encodedPassword: EncodedPassword = this.encodedPassword,
        status: UserStatus = this.status,
        role: UserRole = this.role,
        createdAt: Instant = this.createdAt,
        modifiedAt: Instant = this.modifiedAt,
    ): User {
        return User(
            id = id,
            email = email,
            nickname = nickname,
            encodedPassword = encodedPassword,
            status = status,
            role = role,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
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

    fun isPending(): Boolean {
        return status == UserStatus.PENDING
    }

    fun isActive(): Boolean {
        return status == UserStatus.ACTIVE
    }

    override fun toString(): String {
        return "User(id='$id', email=${email.value}, nickname=${nickname.value}, " +
            "status=$status, role=$role, createdAt=$createdAt, modifiedAt=$modifiedAt)"
    }

    fun activate(modifiedAt: Instant): User {
        return copy(
            status = UserStatus.ACTIVE,
            modifiedAt = modifiedAt,
        )
    }

    companion object {
        fun create(
            id: String,
            email: Email,
            nickname: Nickname,
            encodedPassword: EncodedPassword,
            status: UserStatus = UserStatus.PENDING,
            role: UserRole = UserRole.USER,
            createdAt: Instant,
            modifiedAt: Instant = createdAt,
        ): User {
            return User(
                id = id,
                email = email,
                nickname = nickname,
                encodedPassword = encodedPassword,
                status = status,
                role = role,
                createdAt = createdAt,
                modifiedAt = modifiedAt,
            )
        }
    }
}
