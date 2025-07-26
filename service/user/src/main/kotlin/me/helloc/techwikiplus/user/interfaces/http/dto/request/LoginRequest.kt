package me.helloc.techwikiplus.user.interfaces.http.dto.request

data class LoginRequest(
    val email: String,
    val password: String,
)
