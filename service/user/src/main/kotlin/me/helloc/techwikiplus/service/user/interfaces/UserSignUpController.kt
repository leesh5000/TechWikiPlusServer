package me.helloc.techwikiplus.service.user.interfaces

import me.helloc.techwikiplus.service.user.interfaces.usecase.UserSignUpUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserSignUpController(
    private val userSignUpUseCase: UserSignUpUseCase,
) {
    @PostMapping("/api/v1/users/signup")
    fun signup(
        @RequestBody request: UserSignUpRequest,
    ): ResponseEntity<Void> {
        userSignUpUseCase.execute(
            UserSignUpUseCase.Command(
                email = request.email,
                password = request.password,
                confirmPassword = request.confirmPassword,
                nickname = request.nickname,
            ),
        )

        val headers = HttpHeaders()
        headers.add(HttpHeaders.LOCATION, "/api/v1/users/verify")

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .headers(headers)
            .build()
    }

    data class UserSignUpRequest(
        val email: String,
        val nickname: String,
        val password: String,
        val confirmPassword: String,
    )
}
