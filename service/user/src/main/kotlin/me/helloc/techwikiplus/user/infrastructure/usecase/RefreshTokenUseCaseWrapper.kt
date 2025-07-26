package me.helloc.techwikiplus.user.infrastructure.usecase

import me.helloc.techwikiplus.user.application.RefreshTokenUseCase
import me.helloc.techwikiplus.user.application.TokenResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RefreshTokenUseCaseWrapper(
    private val refreshTokenUseCase: RefreshTokenUseCase,
) {
    fun refresh(refreshToken: String): TokenResult {
        return refreshTokenUseCase.refresh(refreshToken)
    }
}
