package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.ResendVerificationCodeUseCase
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ResendVerificationCodeController(val facade: ResendVerificationCodeUseCase) {

    @PostMapping("/api/v1/users/signup/verify/resend")
    fun resendVerificationCode(@RequestParam(name = "email", required = true) email: String): ResponseEntity<Void> {
        facade.resendVerificationCode(email)
        return ResponseEntity.status(ACCEPTED).build()
    }
}
