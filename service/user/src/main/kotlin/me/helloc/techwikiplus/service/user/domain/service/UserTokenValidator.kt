package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.service.port.TokenValidator
import me.helloc.techwikiplus.service.user.domain.service.port.TokenValidator.TokenClaims

class UserTokenValidator(
    private val tokenValidator: TokenValidator,
) {
    fun validateRefreshTokenOrThrows(token: String): TokenClaims {
        return tokenValidator.validateRefreshTokenOrThrows(token)
    }
}
