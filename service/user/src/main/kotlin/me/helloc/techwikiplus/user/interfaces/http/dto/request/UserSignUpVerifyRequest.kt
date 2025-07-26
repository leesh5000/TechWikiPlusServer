package me.helloc.techwikiplus.user.interfaces.http.dto.request

data class UserSignUpVerifyRequest(
    val email: String,
    val code: String,
)
