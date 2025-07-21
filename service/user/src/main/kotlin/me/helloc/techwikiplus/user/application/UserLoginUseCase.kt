package me.helloc.techwikiplus.user.application

interface UserLoginUseCase {
    fun login(email: String, password: String): LoginResult

    data class LoginResult(
        val accessToken: String,
        val refreshToken: String,
        val userId: Long,
        val email: String,
        val nickname: String
    )
}