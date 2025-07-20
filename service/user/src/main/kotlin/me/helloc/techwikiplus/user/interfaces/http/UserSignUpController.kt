package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.application.UserSignUpFacade
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder

@RestController
class UserSignUpController(val facade: UserSignUpFacade) {

    @PostMapping("/api/v1/users/signup")
    fun signUp(@RequestBody request: UserSignUpRequest): ResponseEntity<Void> {
        facade.signUp(request)
        val createdUri = UriComponentsBuilder
            .fromPath("/api/v1/users/${request.email}")
            .build()
            .toUri()
        return ResponseEntity.created(createdUri).build()
    }

    data class UserSignUpRequest(
        val email: String,
        val nickname: String,
        val password: String,
    )
}
