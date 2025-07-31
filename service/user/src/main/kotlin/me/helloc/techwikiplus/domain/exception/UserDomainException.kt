package me.helloc.techwikiplus.domain.exception

sealed class UserDomainException(message: String) : Exception(message)

class UserAlreadyExistsException(email: String) :
    UserDomainException("User with email $email already exists")

class UserNotActiveException(userId: String) :
    UserDomainException("User $userId is not active")

class InvalidCredentialsException :
    UserDomainException("Invalid email or password")

class UserNotFoundException(identifier: String) :
    UserDomainException("User not found: $identifier")

class PasswordPolicyViolationException(reason: String) :
    UserDomainException("Password does not meet requirements: $reason")