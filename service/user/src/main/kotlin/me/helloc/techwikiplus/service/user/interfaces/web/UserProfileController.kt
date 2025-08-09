package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.interfaces.web.port.UserProfileUseCase
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@Validated
class UserProfileController(
    private val useCase: UserProfileUseCase,
) {
    @GetMapping("/api/v1/users/{userId}", produces = ["application/json"])
    fun getUserProfile(
        @PathVariable("userId") userId: String,
    ): ResponseEntity<Response> =
        UserId(userId)
            .let(useCase::execute)
            .let(Response::from)
            .let { ResponseEntity.ok(it) }

    data class Response(
        val userId: String,
        val email: String,
        val nickname: String,
        val role: String,
        val status: String,
        val createdAt: Instant,
        val modifiedAt: Instant,
    ) {
        companion object {
            fun from(result: UserProfileUseCase.Result): Response =
                Response(
                    userId = result.userId.value,
                    email = result.email,
                    nickname = result.nickname,
                    role = result.role.name,
                    status = result.status.name,
                    createdAt = result.createdAt,
                    modifiedAt = result.modifiedAt,
                )
        }
    }
}
