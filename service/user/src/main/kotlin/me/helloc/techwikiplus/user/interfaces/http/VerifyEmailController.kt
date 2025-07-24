package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.VerifyEmailUseCase
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class VerifyEmailController(
    private val facade: VerifyEmailUseCase,
) {
    @PostMapping("/api/v1/users/signup/verify")
    fun verifyEmail(
        @RequestBody request: UserSignUpVerifyRequest,
    ): ResponseEntity<Void> {
        facade.verify(
            email = request.email,
            code = request.code,
        )
        return ResponseEntity.status(OK).build()
    }

    data class UserSignUpVerifyRequest(
        val email: String,
        val code: String,
    )
}
