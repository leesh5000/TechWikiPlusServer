package me.helloc.techwikiplus.service.user.domain.exception

enum class ErrorCode {
    // User Status
    USER_DORMANT,
    USER_BANNED,
    USER_PENDING,
    USER_DELETED,

    // User Management
    DUPLICATE_EMAIL,
    DUPLICATE_NICKNAME,
    USER_NOT_FOUND,
    PENDING_USER_NOT_FOUND,

    // Authentication
    INVALID_CREDENTIALS,
    PASSWORD_MISMATCH,

    // Token
    INVALID_TOKEN,
    TOKEN_EXPIRED,
    INVALID_TOKEN_TYPE,

    // Verification
    INVALID_VERIFICATION_CODE,
    REGISTRATION_NOT_FOUND,
    CODE_MISMATCH,

    // Notification
    NOTIFICATION_FAILED,

    // Application Level
    SIGNUP_FAILED,
    LOGIN_FAILED,
    VERIFICATION_FAILED,

    // Email Validation
    BLANK_EMAIL,
    INVALID_EMAIL_FORMAT,

    // Nickname Validation
    BLANK_NICKNAME,
    NICKNAME_TOO_SHORT,
    NICKNAME_TOO_LONG,
    NICKNAME_CONTAINS_SPACE,
    NICKNAME_CONTAINS_SPECIAL_CHAR,

    // Password Validation
    BLANK_PASSWORD,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    PASSWORD_NO_UPPERCASE,
    PASSWORD_NO_LOWERCASE,
    PASSWORD_NO_SPECIAL_CHAR,

    // UserId Validation
    BLANK_USER_ID,
    USER_ID_TOO_LONG,

    // Generic
    VALIDATION_ERROR,
    DOMAIN_ERROR,
    INTERNAL_ERROR,
}
