package me.helloc.techwikiplus.service.user.interfaces.web.port

import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import java.time.Instant

interface UserProfileUseCase {
    fun execute(userId: UserId): Result

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
