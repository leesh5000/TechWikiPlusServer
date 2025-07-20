package me.helloc.techwikiplus.user.domain.exception

sealed class CustomException(val errorCode: String, override val message: String) : RuntimeException(message) {

    // 유효성 검증 예외
    sealed class ValidationException(override val message: String) : CustomException(errorCode = "VALIDATION_FAILED", message) {
        data class InvalidEmail(val email: String) : ValidationException("Invalid email format. Your input: $email")
        data class InvalidNickname(val nickname: String) : ValidationException("Nickname must be 2-20 characters long and can only contain alphanumeric characters and Korean characters. Your input: $nickname")
        data class InvalidPassword(val password: String) : ValidationException("Password must be 8-30 characters long and include uppercase, lowercase, numbers, and special characters. Your input: $password")
    }

    // 리소스 찾기 실패 예외
    sealed class NotFoundException(override val message: String) : CustomException(
        errorCode = "NOT_FOUND",
        message)

    // 중복 리소스 예외
    sealed class ConflictException(override val message: String) : CustomException(
        errorCode = "CONFLICT",
        message) {
        data class DuplicateEmail(val email: String) : ConflictException("Email already exists. Your input: $email")
        data class DuplicateNickname(val nickname: String) : ConflictException("Nickname already exists. Your input: $nickname")
    }
}
