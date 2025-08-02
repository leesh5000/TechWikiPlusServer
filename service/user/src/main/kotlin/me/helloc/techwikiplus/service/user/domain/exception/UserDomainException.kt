package me.helloc.techwikiplus.service.user.domain.exception

sealed class UserDomainException(message: String) : Exception(message)

class UserAlreadyExistsException(email: String) :
    UserDomainException("User with email $email already exists")

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
