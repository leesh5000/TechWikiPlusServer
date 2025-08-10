package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserVerifyUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserVerifyController(
    private val useCase: UserVerifyUseCase,
) {
    @PostMapping("/api/v1/users/verify")
    fun verify(
        @RequestBody request: Request,
    ): ResponseEntity<Void> {
        useCase.execute(
            email = Email(request.email),
            code = RegistrationCode(request.registrationCode),
        )

        val headers = HttpHeaders()
        headers.add(HttpHeaders.LOCATION, "/api/v1/users/login")

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(headers)
            .build()
    }

    data class Request(
        val email: String,
        val registrationCode: String,
    )
}
