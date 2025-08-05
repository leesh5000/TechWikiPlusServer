package me.helloc.techwikiplus.service.user.adapter.inbound.web

import me.helloc.techwikiplus.service.user.application.port.inbound.UserLoginUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class UserLoginController(
    private val useCase: UserLoginUseCase,
) {
    @PostMapping("/api/v1/users/login")
    fun login(
        @RequestBody request: Request,
    ): ResponseEntity<Response> {
        val command =
            UserLoginUseCase.Command(
                email = request.email,
                password = request.password,
            )

        val result = useCase.execute(command)

        val response =
            Response(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                userId = result.userId,
                accessTokenExpiresAt = result.accessTokenExpiresAt,
                refreshTokenExpiresAt = result.refreshTokenExpiresAt,
            )

        return ResponseEntity.ok(response)
    }

    data class Request(
        val email: String,
        val password: String,
    )

    data class Response(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val accessTokenExpiresAt: Instant,
        val refreshTokenExpiresAt: Instant,
    )
}
