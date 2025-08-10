package me.helloc.techwikiplus.service.user.domain.model

import me.helloc.techwikiplus.service.user.domain.model.value.UserId

data class UserToken(
    val userId: UserId,
    val token: String,
    val expiresAt: Long,
)
