package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.user.interfaces.web.port.MyProfileUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MyProfileController(
    private val userCase: MyProfileUseCase,
) {
    @GetMapping("/api/v1/users/me")
    fun getMyProfile(): ResponseEntity<Response> {
        val result = userCase.execute()
        return ResponseEntity.ok(Response.from(result))
    }

    data class Response(
        val userId: String,
        val email: String,
        val nickname: String,
        val role: String,
        val status: String,
        val createdAt: String,
        val modifiedAt: String,
    ) {
        companion object {
            fun from(result: MyProfileUseCase.Result): Response {
                return Response(
                    userId = result.userId.value.toString(),
                    email = result.email,
                    nickname = result.nickname,
                    role = result.role.name,
                    status = result.status.name,
                    createdAt = result.createdAt.toString(),
                    modifiedAt = result.modifiedAt.toString(),
                )
            }
        }
    }
}
