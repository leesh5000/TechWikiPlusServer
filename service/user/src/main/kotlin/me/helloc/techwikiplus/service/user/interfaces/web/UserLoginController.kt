package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserLoginUseCase
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
        val result =
            useCase.execute(
                email = Email(value = request.email),
                password = RawPassword(value = request.password),
            )
        val response = Response.from(result)
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
        // ISO-8601 표준
        val accessTokenExpiresAt: String = Instant.now().toString(),
        // ISO-8601 표준
        val refreshTokenExpiresAt: String = Instant.now().toString(),
    ) {
        companion object {
            fun from(result: UserLoginUseCase.Result): Response {
                return Response(
                    accessToken = result.accessToken.token,
                    refreshToken = result.refreshToken.token,
                    userId = result.userId.value.toString(),
                    accessTokenExpiresAt = Instant.ofEpochMilli(result.accessToken.expiresAt).toString(),
                    refreshTokenExpiresAt = Instant.ofEpochMilli(result.refreshToken.expiresAt).toString(),
                )
            }
        }
    }
}
