package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserVerifyResendUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserVerifyResendController(
    private val userCase: UserVerifyResendUseCase,
) {
    @PostMapping("/api/v1/users/verify/resend")
    fun resend(
        @RequestBody request: Request,
    ): ResponseEntity<Void> {
        userCase.execute(
            email = Email(request.email),
        )

        val headers = HttpHeaders()
        headers.add(HttpHeaders.LOCATION, "/api/v1/users/verify")

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(headers)
            .build()
    }

    data class Request(val email: String)
}
