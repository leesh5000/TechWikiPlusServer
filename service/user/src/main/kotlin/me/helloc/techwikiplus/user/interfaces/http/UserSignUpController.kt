package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.UserSignUpUseCase
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserSignUpController(val facade: UserSignUpUseCase) {

    @PostMapping("/api/v1/users/signup")
    fun signUp(@RequestBody request: UserSignUpRequest): ResponseEntity<Void> {
        facade.signUp(
            email = request.email,
            nickname = request.nickname,
            password = request.password,
        )
        return ResponseEntity.status(ACCEPTED)
            .header("Location", "/api/v1/users/signup/verify")
            .build()
    }

    data class UserSignUpRequest(
        val email: String,
        val nickname: String,
        val password: String,
    )
}
