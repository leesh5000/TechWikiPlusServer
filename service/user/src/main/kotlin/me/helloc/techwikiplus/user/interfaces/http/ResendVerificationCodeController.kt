package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.UserSignUpUseCase
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
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



    @PostMapping("/api/v1/users/signup/verify/resend")
    fun resendVerificationCode(@RequestParam(name = "email", required = true) email: String): ResponseEntity<Void> {
        facade.resendVerificationCode(email)
        return ResponseEntity.status(ACCEPTED).build()
    }

    data class UserSignUpRequest(
        val email: String,
        val nickname: String,
        val password: String,
    )

    data class UserSignUpVerifyRequest(
        val email: String,
        val code: String,
    )
}
