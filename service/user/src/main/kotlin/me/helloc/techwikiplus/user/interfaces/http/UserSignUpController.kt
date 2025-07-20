package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.UserSignUpFacade
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class UserSignUpController(val facade: UserSignUpFacade) {

    @PostMapping("/api/v1/users/signup")
    fun signUp(@RequestBody request: UserSignUpRequest): ResponseEntity<Void> {
        facade.signUp(
            email = request.email,
            nickname = request.nickname,
            password = request.password,
        )
        return ResponseEntity.status(ACCEPTED)
            .header("Location", "/api/v1/users/signup/verify?email=${request.email}")
            .build()
    }

    @GetMapping("/api/v1/users/signup/verify")
    fun verifyEmail(@RequestParam(name = "email", required = true) email: String): ResponseEntity<Void> {
        facade.verifyEmail(email)
        return ResponseEntity.status(ACCEPTED).build()
    }

    data class UserSignUpRequest(
        val email: String,
        val nickname: String,
        val password: String,
    )
}
