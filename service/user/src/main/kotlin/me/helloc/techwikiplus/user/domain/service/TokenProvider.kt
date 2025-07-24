package me.helloc.techwikiplus.user.domain.service

interface TokenProvider : TokenGenerator, TokenValidator, TokenParser
