package me.helloc.techwikiplus.user.interfaces.http.dto.response

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
)
