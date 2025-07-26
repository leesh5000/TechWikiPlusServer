package me.helloc.techwikiplus.user.domain.exception

enum class ErrorCode(
    val code: String,
    val message: String,
) {
    // Validation Errors (USER_001 ~ USER_099)
    INVALID_EMAIL("USER_001", "Invalid email format"),
    INVALID_NICKNAME("USER_002", "Invalid nickname format"),
    INVALID_PASSWORD("USER_003", "Invalid password format"),
    ALREADY_VERIFIED_EMAIL("USER_004", "Email is already verified"),

    // Not Found Errors (USER_101 ~ USER_199)
    USER_NOT_FOUND("USER_101", "User not found"),
    RESOURCE_NOT_FOUND("USER_102", "Resource not found"),

    // Conflict Errors (USER_201 ~ USER_299)
    DUPLICATE_EMAIL("USER_201", "Email already exists"),
    DUPLICATE_NICKNAME("USER_202", "Nickname already exists"),

    // Authentication Errors (AUTH_001 ~ AUTH_099)
    EXPIRED_EMAIL_VERIFICATION("AUTH_001", "Email verification expired"),
    PENDING_USER_NOT_FOUND("AUTH_002", "Pending user not found"),
    INVALID_VERIFICATION_CODE("AUTH_003", "Invalid verification code"),
    INVALID_CREDENTIALS("AUTH_004", "Invalid email or password"),
    UNAUTHORIZED_ACCESS("AUTH_005", "Unauthorized access"),
    EMAIL_NOT_VERIFIED("AUTH_006", "Email not verified"),
    INVALID_TOKEN("AUTH_007", "Invalid token"),
    INVALID_TOKEN_TYPE("AUTH_008", "Invalid token type"),
    ACCOUNT_BANNED("AUTH_009", "Account has been banned"),
    ACCOUNT_DORMANT("AUTH_010", "Account is dormant"),
    ACCOUNT_DELETED("AUTH_011", "Account has been deleted"),

    // Rate Limit Errors (RATE_001 ~ RATE_099)
    RATE_LIMIT_EXCEEDED("RATE_001", "Rate limit exceeded"),
}
