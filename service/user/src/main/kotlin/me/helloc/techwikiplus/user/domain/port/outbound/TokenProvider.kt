package me.helloc.techwikiplus.user.domain.port.outbound

interface TokenProvider : TokenGenerator, TokenValidator, TokenParser
