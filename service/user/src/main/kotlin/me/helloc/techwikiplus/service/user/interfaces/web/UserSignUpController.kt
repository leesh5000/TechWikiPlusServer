package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserSignUpUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserSignUpController(
    private val useCase: UserSignUpUseCase,
) {
    @PostMapping("/api/v1/users/signup")
    fun signup(
        @RequestBody request: Request,
    ): ResponseEntity<Void> {
        useCase.execute(
            email = Email(request.email),
            nickname = Nickname(request.nickname),
            password = RawPassword(request.password),
            confirmPassword = RawPassword(request.confirmPassword),
        )

        val headers = HttpHeaders()
        headers.add(HttpHeaders.LOCATION, "/api/v1/users/verify")

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .headers(headers)
            .build()
    }

    data class Request(
        val email: String,
        val nickname: String,
        val password: String,
        val confirmPassword: String,
    )
}
