package me.helloc.techwikiplus.domain.service

import me.helloc.techwikiplus.domain.model.User
import me.helloc.techwikiplus.domain.model.result.DomainResult
import me.helloc.techwikiplus.domain.model.type.UserStatus
import me.helloc.techwikiplus.domain.model.value.Email
import me.helloc.techwikiplus.domain.service.port.UserRepository

class UserAuthenticationService(
    private val userRepository: UserRepository,
    private val passwordService: UserPasswordService,
) {
    fun authenticate(email: Email, rawPassword: String): DomainResult<User> {
        return try {
            val user = userRepository.findByEmail(email)
                ?: return DomainResult.Failure.NotFound("User", email.value)
            
            if (!passwordService.matches(rawPassword, user.password.value)) {
                return DomainResult.Failure.Unauthorized("Invalid credentials")
            }
            
            when (user.status) {
                UserStatus.ACTIVE -> DomainResult.Success(user)
                UserStatus.DORMANT -> DomainResult.Failure.BusinessRuleViolation("User account is not active")
                UserStatus.BANNED -> DomainResult.Failure.BusinessRuleViolation("User account is banned")
                UserStatus.PENDING -> DomainResult.Failure.BusinessRuleViolation("User account is pending activation")
                UserStatus.DELETED -> DomainResult.Failure.BusinessRuleViolation("User account is deleted")
            }
        } catch (e: Exception) {
            DomainResult.Failure.SystemError(e.message ?: "Unknown error occurred")
        }
    }
}