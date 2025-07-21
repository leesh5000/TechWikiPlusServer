package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.UserLoginUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserLoginController(
    private val userLoginUseCase: UserLoginUseCase
) {

    @PostMapping("/api/v1/users/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val result = userLoginUseCase.login(
            email = request.email,
            password = request.password
        )
        
        return ResponseEntity.ok(
            LoginResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                user = UserInfo(
                    id = result.userId,
                    email = result.email,
                    nickname = result.nickname
                )
            )
        )
    }

    data class LoginRequest(
        val email: String,
        val password: String
    )

    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String,
        val user: UserInfo
    )

    data class UserInfo(
        val id: Long,
        val email: String,
        val nickname: String
    )
}