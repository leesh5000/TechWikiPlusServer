package me.helloc.techwikiplus.user.interfaces.http.dto.response

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
)
