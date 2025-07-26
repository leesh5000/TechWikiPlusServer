package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.infrastructure.usecase.ResendVerificationCodeUseCaseWrapper
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ResendVerificationCodeController(val facade: ResendVerificationCodeUseCaseWrapper) {
    @GetMapping("/api/v1/users/signup/verify/resend")
    fun resendVerificationCode(
        @RequestParam(name = "email", required = true) email: String,
    ): ResponseEntity<Void> {
        facade.resendVerificationCode(email)
        return ResponseEntity.status(ACCEPTED).build()
    }
}
