package me.helloc.techwikiplus.user.infrastructure.config

import me.helloc.techwikiplus.user.domain.service.TokenGenerator
import me.helloc.techwikiplus.user.domain.service.TokenParser
import me.helloc.techwikiplus.user.domain.service.TokenProvider
import me.helloc.techwikiplus.user.domain.service.TokenValidator
import me.helloc.techwikiplus.user.infrastructure.security.JwtTokenProvider
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
