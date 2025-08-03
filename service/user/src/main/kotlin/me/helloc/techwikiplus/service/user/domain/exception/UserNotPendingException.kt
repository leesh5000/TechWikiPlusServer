package me.helloc.techwikiplus.service.user.domain.exception

class UserNotPendingException(
    email: String,
) : UserDomainException("User with email $email is not in pending status")
