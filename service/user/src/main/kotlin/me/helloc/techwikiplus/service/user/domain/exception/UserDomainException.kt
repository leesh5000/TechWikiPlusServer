package me.helloc.techwikiplus.service.user.domain.exception

import me.helloc.techwikiplus.service.user.domain.model.value.Email

open class UserDomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class DormantUserException(email: Email) :
    UserDomainException("User with email ${email.value} is dormant. Please contact the administrator.")

class BannedUserException(email: Email) :
    UserDomainException("User with email ${email.value} is banned. Please contact the administrator.")

class PendingUserException(email: Email) :
    UserDomainException(
        "User with email ${email.value} is pending activation. Please check your email for the verification code.",
    )

class DeletedUserException(email: Email) :
    UserDomainException("User with email ${email.value} has been deleted. Please contact the administrator.")
