package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.TokenType

interface TokenValidator {
    fun validateToken(token: String): Boolean

    fun getTokenType(token: String): TokenType
}
