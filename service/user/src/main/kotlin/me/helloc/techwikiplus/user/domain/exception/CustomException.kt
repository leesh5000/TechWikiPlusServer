package me.helloc.techwikiplus.user.domain.exception

import me.helloc.techwikiplus.user.domain.DomainConstants

sealed class CustomException(val errorCode: String, override val message: String) : RuntimeException(message) {
    companion object {
        private const val INPUT_FORMAT = "Your input: %s"
    }

    // 유효성 검증 예외
    sealed class ValidationException(
        override val message: String,
    ) : CustomException(errorCode = "VALIDATION_FAILED", message) {
        data class InvalidEmail(val email: String) : ValidationException(
            "Invalid email format. ${INPUT_FORMAT.format(email)}",
        )

        data class InvalidNickname(val nickname: String) : ValidationException(
            "Nickname must be ${DomainConstants.Nickname.MIN_LENGTH}-" +
                "${DomainConstants.Nickname.MAX_LENGTH} characters long and can only contain " +
                "alphanumeric characters and Korean characters. ${INPUT_FORMAT.format(nickname)}",
        )

        data class InvalidPassword(val password: String) : ValidationException(
            "Password must be ${DomainConstants.Password.MIN_LENGTH}-" +
                "${DomainConstants.Password.MAX_LENGTH} characters long and include uppercase, " +
                "lowercase, numbers, and special characters. ${INPUT_FORMAT.format(password)}",
        )

        data class AlreadyVerifiedEmail(val email: String) : ValidationException(
            "Email is already verified. ${INPUT_FORMAT.format(email)}",
        )
    }

    // 리소스 찾기 실패 예외
    open class NotFoundException(override val message: String) : CustomException(errorCode = "NOT_FOUND", message) {
        data class UserEmailNotFoundException(
            val email: String,
        ) : NotFoundException("User not found with email: $email")

        data class ResourceNotFoundException(val resource: String) : NotFoundException("Resource not found: $resource")
    }

    // 중복 리소스 예외
    sealed class ConflictException(override val message: String) : CustomException(
        errorCode = "CONFLICT",
        message,
    ) {
        data class DuplicateEmail(val email: String) : ConflictException(
            "Email already exists. ${INPUT_FORMAT.format(email)}",
        )

        data class DuplicateNickname(val nickname: String) : ConflictException(
            "Nickname already exists. ${INPUT_FORMAT.format(nickname)}",
        )
    }

    // 인증 관련 예외
    sealed class AuthenticationException(
        override val message: String,
    ) : CustomException(errorCode = "AUTHENTICATION_FAILED", message) {
        data class ExpiredEmailVerification(
            val email: String,
        ) : AuthenticationException(
                "Email verification expired for email: $email. Please request a new verification code.",
            )

        data class PendingUserNotFound(val email: String) : AuthenticationException(
            "Pending user not found for email: $email. Please ensure you have registered and requested verification.",
        )

        data class InvalidVerificationCode(val code: String) : AuthenticationException(
            "Invalid verification code: $code. Please check the code and try again.",
        )

        class InvalidCredentials : AuthenticationException("Invalid email or password")

        data class UnauthorizedAccess(val resource: String) : AuthenticationException(
            "Unauthorized access to resource: $resource",
        )

        class EmailNotVerified : AuthenticationException(
            "Email not verified. Please verify your email before logging in.",
        )

        class InvalidToken : AuthenticationException("Invalid refresh token")

        class InvalidTokenType : AuthenticationException("Expected refresh token but received access token")

        class AccountBanned : AuthenticationException("Your account has been banned.")

        class AccountDormant : AuthenticationException("Your account is dormant. Please contact support to reactivate.")

        class AccountDeleted : AuthenticationException("Your account has been deleted.")
    }

    // Rate Limit 예외
    data class ResendRateLimitExceeded(
        override val message: String,
    ) : CustomException(errorCode = "RATE_LIMIT_EXCEEDED", message)
}
