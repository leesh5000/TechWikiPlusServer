package me.helloc.techwikiplus.service.user.domain.exception

import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname

sealed class UserDomainException(message: String) : Exception(message)

sealed class UserAlreadyExistsException(message: String) : UserDomainException(message) {
    class ForEmail(email: Email) :
        UserAlreadyExistsException("User with email ${email.value} already exists")

    class ForNickname(nickname: Nickname) :
        UserAlreadyExistsException("User with nickname ${nickname.value} already exists")
}

class UserNotActiveException(reason: String) :
    UserDomainException(reason)

class InvalidCredentialsException :
    UserDomainException("Invalid email or password")

class UserNotFoundException(identifier: String) :
    UserDomainException("User not found: $identifier")

class PasswordPolicyViolationException(reason: String) :
    UserDomainException("Password does not meet requirements: $reason")

class PasswordMismatchException(reason: String) :
    UserDomainException("Password and confirmation do not match: $reason")

class PendingUserException() :
    UserDomainException("User is pending activation. Please check your email for the verification code.")

class DormantUserException() :
    UserDomainException("User is dormant. Please contact the administrator.")

class BannedUserException() :
    UserDomainException("User is banned. Please contact the administrator.")
