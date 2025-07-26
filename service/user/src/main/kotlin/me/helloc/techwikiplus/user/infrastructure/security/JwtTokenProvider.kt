package me.helloc.techwikiplus.user.infrastructure.security

import me.helloc.techwikiplus.user.domain.port.outbound.TokenGenerator
import me.helloc.techwikiplus.user.domain.port.outbound.TokenParser
import me.helloc.techwikiplus.user.domain.port.outbound.TokenProvider
import me.helloc.techwikiplus.user.domain.port.outbound.TokenValidator

class JwtTokenProvider(
    private val tokenGenerator: TokenGenerator,
    private val tokenValidator: TokenValidator,
    private val tokenParser: TokenParser,
) : TokenProvider, TokenGenerator by tokenGenerator, TokenValidator by tokenValidator, TokenParser by tokenParser
