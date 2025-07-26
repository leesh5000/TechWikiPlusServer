package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.infrastructure.usecase.UserSignUpUseCaseWrapper
import me.helloc.techwikiplus.user.interfaces.http.dto.request.UserSignUpRequest
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserSignUpController(val facade: UserSignUpUseCaseWrapper) {
    @PostMapping("/api/v1/users/signup", consumes = ["application/json"])
    fun signUp(
        @RequestBody request: UserSignUpRequest,
    ): ResponseEntity<Void> {
        facade.signUp(
            email = request.email,
            nickname = request.nickname,
            password = request.password,
        )
        return ResponseEntity.status(ACCEPTED)
            .header("Location", "/api/v1/users/signup/verify")
            .build()
    }
}
