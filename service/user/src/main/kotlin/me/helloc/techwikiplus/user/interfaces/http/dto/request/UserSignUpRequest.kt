package me.helloc.techwikiplus.user.interfaces.http.dto.request

data class UserSignUpRequest(
    val email: String,
    val nickname: String,
    val password: String,
)
