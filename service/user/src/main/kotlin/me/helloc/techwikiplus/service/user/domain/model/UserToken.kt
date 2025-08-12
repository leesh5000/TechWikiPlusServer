package me.helloc.techwikiplus.service.user.domain.model

data class UserToken(
    val userId: UserId,
    val token: String,
    val expiresAt: Long,
)
