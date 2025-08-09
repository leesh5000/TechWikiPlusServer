package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import java.time.Instant

interface UserProfileUseCase {
    fun execute(targetUserId: UserId): Result

    data class Result(
        val userId: UserId,
        val email: String,
        val nickname: String,
        val role: UserRole,
        val status: UserStatus,
        val createdAt: Instant,
        val modifiedAt: Instant,
    )
}
