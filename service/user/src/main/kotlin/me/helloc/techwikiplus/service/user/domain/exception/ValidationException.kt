package me.helloc.techwikiplus.service.user.domain.exception

abstract class ValidationException(
    val field: String,
    val errorCode: String,
    message: String,
) : UserDomainException(message)

class EmailValidationException(
    errorCode: String,
    message: String,
) : ValidationException(
        field = "email",
        errorCode = errorCode,
        message = message,
    ) {
    companion object {
        const val BLANK_EMAIL = "BLANK_EMAIL"
        const val INVALID_FORMAT = "INVALID_EMAIL_FORMAT"
    }
}

class NicknameValidationException(
    errorCode: String,
    message: String,
) : ValidationException(
        field = "nickname",
        errorCode = errorCode,
        message = message,
    ) {
    companion object {
        const val BLANK_NICKNAME = "BLANK_NICKNAME"
        const val TOO_SHORT = "NICKNAME_TOO_SHORT"
        const val TOO_LONG = "NICKNAME_TOO_LONG"
        const val CONTAINS_SPACE = "NICKNAME_CONTAINS_SPACE"
        const val CONTAINS_SPECIAL_CHAR = "NICKNAME_CONTAINS_SPECIAL_CHAR"
    }
}

class PasswordValidationException(
    errorCode: String,
    message: String,
) : ValidationException(
        field = "password",
        errorCode = errorCode,
        message = message,
    ) {
    companion object {
        const val BLANK_PASSWORD = "BLANK_PASSWORD"
        const val TOO_SHORT = "PASSWORD_TOO_SHORT"
        const val TOO_LONG = "PASSWORD_TOO_LONG"
        const val NO_UPPERCASE = "PASSWORD_NO_UPPERCASE"
        const val NO_LOWERCASE = "PASSWORD_NO_LOWERCASE"
        const val NO_SPECIAL_CHAR = "PASSWORD_NO_SPECIAL_CHAR"
    }
}

class UserIdValidationException(
    errorCode: String,
    message: String,
) : ValidationException(
        field = "userId",
        errorCode = errorCode,
        message = message,
    ) {
    companion object {
        const val BLANK_USER_ID = "BLANK_USER_ID"
        const val INVALID_FORMAT = "INVALID_USER_ID_FORMAT"
    }
}
