package me.helloc.techwikiplus.user.infrastructure.security

import me.helloc.techwikiplus.user.domain.port.outbound.TokenGenerator
import me.helloc.techwikiplus.user.domain.port.outbound.TokenParser
import me.helloc.techwikiplus.user.domain.port.outbound.TokenProvider
import me.helloc.techwikiplus.user.domain.port.outbound.TokenValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class TokenConfig {
    @Bean
    @Primary
    fun tokenProvider(
        tokenGenerator: TokenGenerator,
        tokenValidator: TokenValidator,
        tokenParser: TokenParser,
    ): TokenProvider {
        return JwtTokenProvider(tokenGenerator, tokenValidator, tokenParser)
    }
}
