package me.helloc.techwikiplus.service.user.interfaces.web

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import jakarta.servlet.http.HttpServletRequest
import me.helloc.techwikiplus.service.user.application.exception.ApplicationException
import me.helloc.techwikiplus.service.user.application.exception.LoginFailedException
import me.helloc.techwikiplus.service.user.application.exception.SignUpFailedException
import me.helloc.techwikiplus.service.user.application.exception.VerificationFailedException
import me.helloc.techwikiplus.service.user.domain.exception.ActiveUserNotFoundException
import me.helloc.techwikiplus.service.user.domain.exception.BannedUserException
import me.helloc.techwikiplus.service.user.domain.exception.DeletedUserException
import me.helloc.techwikiplus.service.user.domain.exception.DormantUserException
import me.helloc.techwikiplus.service.user.domain.exception.ExpiredTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidTokenTypeException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidVerificationCodeException
import me.helloc.techwikiplus.service.user.domain.exception.MailSendException
import me.helloc.techwikiplus.service.user.domain.exception.PasswordsDoNotMatchException
import me.helloc.techwikiplus.service.user.domain.exception.PendingUserException
import me.helloc.techwikiplus.service.user.domain.exception.PendingUserNotFoundException
import me.helloc.techwikiplus.service.user.domain.exception.RegistrationCodeMismatchException
import me.helloc.techwikiplus.service.user.domain.exception.RegistrationEmailNotFoundException
import me.helloc.techwikiplus.service.user.domain.exception.UserAlreadyExistsException
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotificationFailedException
import me.helloc.techwikiplus.service.user.domain.exception.ValidationException
import me.helloc.techwikiplus.service.user.interfaces.web.dto.SafeErrorResponse
import me.helloc.techwikiplus.service.user.interfaces.web.security.InputSanitizer
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(
    private val inputSanitizer: InputSanitizer,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Helper method to create safe error responses
    private fun createErrorResponse(
        status: HttpStatus,
        code: String,
        message: String?,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        val safeMessage = message ?: getDefaultMessage(code)

        logger.debug("Creating error response - Status: {}, Code: {}", status, code)

        return ResponseEntity
            .status(status)
            .body(SafeErrorResponse.of(code, safeMessage, request.requestURI))
    }

    private fun getDefaultMessage(code: String): String {
        return when (code) {
            "USER_DORMANT" -> "휴면 계정입니다"
            "USER_BANNED" -> "차단된 계정입니다"
            "USER_PENDING" -> "인증 대기중인 계정입니다"
            "USER_DELETED" -> "삭제된 계정입니다"
            "USER_ALREADY_EXISTS" -> "이미 존재하는 사용자입니다"
            "USER_NOT_FOUND" -> "사용자를 찾을 수 없습니다"
            "PENDING_USER_NOT_FOUND" -> "대기중인 사용자를 찾을 수 없습니다"
            "INVALID_CREDENTIALS" -> "인증 정보가 올바르지 않습니다"
            "PASSWORDS_MISMATCH" -> "비밀번호가 일치하지 않습니다"
            "INVALID_TOKEN" -> "유효하지 않은 토큰입니다"
            "TOKEN_EXPIRED" -> "만료된 토큰입니다"
            "INVALID_TOKEN_TYPE" -> "잘못된 토큰 타입입니다"
            "INVALID_VERIFICATION_CODE" -> "유효하지 않은 인증 코드입니다"
            "REGISTRATION_NOT_FOUND" -> "회원가입 정보를 찾을 수 없습니다"
            "CODE_MISMATCH" -> "인증 코드가 일치하지 않습니다"
            "MAIL_SEND_FAILED" -> "이메일 전송에 실패했습니다"
            "NOTIFICATION_FAILED" -> "알림 전송에 실패했습니다"
            "SIGNUP_FAILED" -> "회원가입 처리 중 오류가 발생했습니다"
            "LOGIN_FAILED" -> "로그인 처리 중 오류가 발생했습니다"
            "VERIFICATION_FAILED" -> "인증 처리 중 오류가 발생했습니다"
            "APPLICATION_ERROR" -> "처리 중 오류가 발생했습니다"
            "DOMAIN_ERROR" -> "도메인 처리 중 오류가 발생했습니다"
            "MISSING_REQUIRED_FIELD" -> "필수 필드가 누락되었습니다"
            "INVALID_REQUEST_BODY" -> "잘못된 요청 형식입니다"
            "INVALID_ARGUMENT" -> "잘못된 인자입니다"
            "INTERNAL_ERROR" -> "시스템 오류가 발생했습니다"
            else -> "오류가 발생했습니다"
        }
    }

    // ========== Domain Exceptions - User Status ==========

    @ExceptionHandler(DormantUserException::class)
    fun handleDormantUser(
        e: DormantUserException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Dormant user attempt detected")
        return createErrorResponse(HttpStatus.FORBIDDEN, "USER_DORMANT", e.message, request)
    }

    @ExceptionHandler(BannedUserException::class)
    fun handleBannedUser(
        e: BannedUserException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Banned user attempt detected")
        return createErrorResponse(HttpStatus.FORBIDDEN, "USER_BANNED", e.message, request)
    }

    @ExceptionHandler(PendingUserException::class)
    fun handlePendingUser(
        e: PendingUserException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Pending user attempt detected")
        return createErrorResponse(HttpStatus.FORBIDDEN, "USER_PENDING", e.message, request)
    }

    @ExceptionHandler(DeletedUserException::class)
    fun handleDeletedUser(
        e: DeletedUserException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Deleted user attempt detected")
        return createErrorResponse(HttpStatus.GONE, "USER_DELETED", e.message, request)
    }

    // ========== Domain Exceptions - User Management ==========

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExists(
        e: UserAlreadyExistsException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("User already exists")
        return createErrorResponse(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", e.message, request)
    }

    @ExceptionHandler(ActiveUserNotFoundException::class)
    fun handleActiveUserNotFound(
        e: ActiveUserNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Active user not found")
        return createErrorResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", e.message, request)
    }

    @ExceptionHandler(PendingUserNotFoundException::class)
    fun handlePendingUserNotFound(
        e: PendingUserNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Pending user not found")
        return createErrorResponse(HttpStatus.NOT_FOUND, "PENDING_USER_NOT_FOUND", e.message, request)
    }

    // ========== Domain Exceptions - Authentication ==========

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(
        e: InvalidCredentialsException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Invalid credentials attempt")
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.message, request)
    }

    @ExceptionHandler(PasswordsDoNotMatchException::class)
    fun handlePasswordsDoNotMatch(
        e: PasswordsDoNotMatchException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Passwords do not match")
        return createErrorResponse(HttpStatus.BAD_REQUEST, "PASSWORDS_MISMATCH", e.message, request)
    }

    // ========== Domain Exceptions - Token ==========

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(
        e: InvalidTokenException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Invalid token detected")
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", e.message, request)
    }

    @ExceptionHandler(ExpiredTokenException::class)
    fun handleExpiredToken(
        e: ExpiredTokenException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Expired token detected")
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", e.message, request)
    }

    @ExceptionHandler(InvalidTokenTypeException::class)
    fun handleInvalidTokenType(
        e: InvalidTokenTypeException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Invalid token type detected")
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_TYPE", e.message, request)
    }

    // ========== Domain Exceptions - Verification ==========

    @ExceptionHandler(InvalidVerificationCodeException::class)
    fun handleInvalidVerificationCode(
        e: InvalidVerificationCodeException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Invalid verification code")
        return createErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", e.message, request)
    }

    @ExceptionHandler(RegistrationEmailNotFoundException::class)
    fun handleRegistrationEmailNotFound(
        e: RegistrationEmailNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Registration email not found")
        return createErrorResponse(HttpStatus.NOT_FOUND, "REGISTRATION_NOT_FOUND", e.message, request)
    }

    @ExceptionHandler(RegistrationCodeMismatchException::class)
    fun handleRegistrationCodeMismatch(
        e: RegistrationCodeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Registration code mismatch")
        return createErrorResponse(HttpStatus.BAD_REQUEST, "CODE_MISMATCH", e.message, request)
    }

    // ========== Domain Exceptions - Validation ==========

    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(
        e: ValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Validation error - field: [SANITIZED], code: {}", e.errorCode)

        val safeDetails = mutableMapOf<String, Any>()
        e.field?.let { field ->
            inputSanitizer.sanitizeFieldName(field)?.let {
                safeDetails["field"] = it
            }
        }
        e.errorCode?.let { code ->
            inputSanitizer.sanitizeFieldName(code)?.let {
                safeDetails["code"] = it
            }
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                SafeErrorResponse.validation(
                    e.message ?: "검증 실패",
                    request.requestURI,
                    safeDetails.ifEmpty { null },
                ),
            )
    }

    // ========== Domain Exceptions - Mail ==========

    @ExceptionHandler(MailSendException::class)
    fun handleMailSendException(
        e: MailSendException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.error("Mail send failed")
        // 사용자에게는 구체적인 에러 메시지를 노출하지 않음
        return createErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "MAIL_SEND_FAILED",
            "이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요",
            request,
        )
    }

    @ExceptionHandler(UserNotificationFailedException::class)
    fun handleUserNotificationFailed(
        e: UserNotificationFailedException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.error("User notification failed")
        return createErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "NOTIFICATION_FAILED", e.message, request)
    }

    // ========== Application Exceptions ==========

    @ExceptionHandler(SignUpFailedException::class)
    fun handleSignUpFailed(
        e: SignUpFailedException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.error("Sign up failed: {}", e.javaClass.simpleName)
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SIGNUP_FAILED", e.message, request)
    }

    @ExceptionHandler(LoginFailedException::class)
    fun handleLoginFailed(
        e: LoginFailedException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.error("Login failed: {}", e.javaClass.simpleName)
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "LOGIN_FAILED", e.message, request)
    }

    @ExceptionHandler(VerificationFailedException::class)
    fun handleVerificationFailed(
        e: VerificationFailedException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.error("Verification failed: {}", e.javaClass.simpleName)
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "VERIFICATION_FAILED", e.message, request)
    }

    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(
        e: ApplicationException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.error("Application error: {}", e.javaClass.simpleName)
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "APPLICATION_ERROR", e.message, request)
    }

    // ========== Catch-all for Domain Exceptions ==========

    @ExceptionHandler(UserDomainException::class)
    fun handleUserDomainException(
        e: UserDomainException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.error("Domain exception: {}", e.javaClass.simpleName)
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "DOMAIN_ERROR", e.message, request)
    }

    // ========== Framework Exceptions ==========

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        e: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Failed to read HTTP message")

        val cause = e.cause
        if (cause is MismatchedInputException) {
            val fieldName = cause.path.firstOrNull()?.fieldName
            val safeFieldName = inputSanitizer.sanitizeFieldName(fieldName) ?: "unknown"
            val safeMessage = "필수 필드가 누락되었습니다: $safeFieldName"

            return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_REQUIRED_FIELD",
                safeMessage,
                request,
            )
        }

        return createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST_BODY",
            "잘못된 요청 형식입니다",
            request,
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        e: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.warn("Illegal argument detected")
        return createErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", e.message, request)
    }

    // ========== Generic Exceptions ==========

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        e: RuntimeException,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.error("Unexpected runtime exception: {}", e.javaClass.simpleName)
        return createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "예기치 않은 오류가 발생했습니다",
            request,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        e: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<SafeErrorResponse> {
        logger.error("Unexpected exception: {}", e.javaClass.simpleName)
        return createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "시스템 오류가 발생했습니다",
            request,
        )
    }
}
