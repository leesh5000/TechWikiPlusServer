package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.UserLoginUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserLoginController(
    private val useCase: UserLoginUseCase
) {

    @PostMapping("/api/v1/users/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val result = useCase.login(
            email = request.email,
            password = request.password
        )

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                userId = result.userId
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
        val userId: Long,
    )
}
