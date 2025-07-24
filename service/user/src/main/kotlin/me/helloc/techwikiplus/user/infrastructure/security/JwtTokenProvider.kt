package me.helloc.techwikiplus.user.infrastructure.security

import me.helloc.techwikiplus.user.domain.service.TokenGenerator
import me.helloc.techwikiplus.user.domain.service.TokenParser
import me.helloc.techwikiplus.user.domain.service.TokenProvider
import me.helloc.techwikiplus.user.domain.service.TokenValidator

class JwtTokenProvider(
    private val tokenGenerator: TokenGenerator,
    private val tokenValidator: TokenValidator,
    private val tokenParser: TokenParser,
) : TokenProvider, TokenGenerator by tokenGenerator, TokenValidator by tokenValidator, TokenParser by tokenParser
