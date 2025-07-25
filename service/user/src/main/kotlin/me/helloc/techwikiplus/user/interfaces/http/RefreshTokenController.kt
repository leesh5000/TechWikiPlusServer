package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.infrastructure.usecase.RefreshTokenUseCaseWrapper
import me.helloc.techwikiplus.user.interfaces.http.dto.request.RefreshTokenRequest
import me.helloc.techwikiplus.user.interfaces.http.dto.response.RefreshTokenResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RefreshTokenController(
    private val useCase: RefreshTokenUseCaseWrapper,
) {
    @PostMapping("/api/v1/users/refresh")
    fun refresh(
        @RequestBody request: RefreshTokenRequest,
    ): ResponseEntity<RefreshTokenResponse> {
        val result = useCase.refresh(request.refreshToken)

        return ResponseEntity.ok(
            RefreshTokenResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                userId = result.userId,
            ),
        )
    }
}
