package me.helloc.techwikiplus.service.user.domain.model

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import java.time.Instant

class User(
    val id: UserId,
    val email: Email,
    val nickname: Nickname,
    val encodedPassword: EncodedPassword,
    val status: UserStatus,
    val role: UserRole,
    val createdAt: Instant,
    val modifiedAt: Instant,
) {
    init {
        // UserId validation is already done in UserId value object
    }

    fun copy(
        id: UserId = this.id,
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
        if (status == UserStatus.ACTIVE) {
            return this // 이미 활성화된 상태면 그대로 반환
        }
        return copy(
            status = UserStatus.ACTIVE,
            modifiedAt = modifiedAt,
        )
    }

    fun setPending(modifiedAt: Instant): User {
        if (status == UserStatus.PENDING) {
            return this // 이미 대기 상태면 그대로 반환
        }
        return copy(
            status = UserStatus.PENDING,
            modifiedAt = modifiedAt,
        )
    }

    fun validateUserStatus() {
        when (status) {
            UserStatus.BANNED -> {
                throw DomainException(ErrorCode.USER_BANNED)
            }
            UserStatus.DELETED -> {
                throw DomainException(ErrorCode.USER_DELETED)
            }
            UserStatus.PENDING -> {
                throw DomainException(ErrorCode.USER_PENDING)
            }
            else -> {}
        }
    }

    companion object {
        fun create(
            id: UserId,
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
