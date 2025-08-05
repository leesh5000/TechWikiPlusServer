package me.helloc.techwikiplus.service.user.adapter.inbound.web

import me.helloc.techwikiplus.service.user.application.port.inbound.UserSignUpUseCase
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
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
        useCase.execute(request.toCommand())

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
    ) {
        fun toCommand(): UserSignUpUseCase.Command {
            return UserSignUpUseCase.Command(
                email = Email(email),
                nickname = Nickname(nickname),
                password = RawPassword(password),
                confirmPassword = RawPassword(confirmPassword),
            )
        }
    }
}
