package me.helloc.techwikiplus.service.user.domain.exception

class InvalidTokenException(message: String = "Invalid token") :
    UserDomainException(message)

class ExpiredTokenException(message: String = "Token has expired") :
    UserDomainException(message)

class InvalidTokenTypeException(
    expected: String,
    actual: String,
) : UserDomainException("Invalid token type. Expected: $expected, but got: $actual")
