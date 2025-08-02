package me.helloc.techwikiplus.service.user.domain.exception

class InvalidVerificationCodeException(
    message: String = "Invalid verification code",
) : UserDomainException(message)
