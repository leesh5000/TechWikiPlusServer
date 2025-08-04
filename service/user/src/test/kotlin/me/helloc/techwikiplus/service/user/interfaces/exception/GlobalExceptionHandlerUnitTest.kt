package me.helloc.techwikiplus.service.user.interfaces.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.EmailValidationException
import me.helloc.techwikiplus.service.user.domain.exception.InvalidCredentialsException
import me.helloc.techwikiplus.service.user.domain.exception.NicknameValidationException
import me.helloc.techwikiplus.service.user.domain.exception.PasswordMismatchException
import me.helloc.techwikiplus.service.user.domain.exception.PasswordPolicyViolationException
import me.helloc.techwikiplus.service.user.domain.exception.PasswordValidationException
import me.helloc.techwikiplus.service.user.domain.exception.UserAlreadyExistsException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotActiveException
import me.helloc.techwikiplus.service.user.domain.exception.UserNotFoundException
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.interfaces.GlobalExceptionHandler
import me.helloc.techwikiplus.service.user.interfaces.GlobalExceptionHandler.ErrorResponse
import me.helloc.techwikiplus.service.user.interfaces.GlobalExceptionHandler.FieldError
import me.helloc.techwikiplus.service.user.interfaces.GlobalExceptionHandler.ValidationErrorResponse
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerUnitTest : FunSpec({

    val handler = GlobalExceptionHandler()

    test("EmailValidationException은 400 BAD_REQUEST와 필드별 에러 정보 반환") {
        // Given
        val exception =
            EmailValidationException(
                errorCode = EmailValidationException.INVALID_FORMAT,
                message = "올바른 이메일 형식이 아닙니다",
            )

        // When
        val response = handler.handleValidationException(exception)

        // Then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.code shouldBe "VALIDATION_ERROR"
        response.body?.message shouldBe "입력값 검증에 실패했습니다"
        response.body?.errors?.size shouldBe 1

        val fieldError = response.body?.errors?.first()
        fieldError?.field shouldBe "email"
        fieldError?.code shouldBe EmailValidationException.INVALID_FORMAT
        fieldError?.message shouldBe "올바른 이메일 형식이 아닙니다"
    }

    test("NicknameValidationException은 400 BAD_REQUEST와 필드별 에러 정보 반환") {
        // Given
        val exception =
            NicknameValidationException(
                errorCode = NicknameValidationException.TOO_SHORT,
                message = "닉네임은 최소 2자 이상이어야 합니다",
            )

        // When
        val response = handler.handleValidationException(exception)

        // Then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.code shouldBe "VALIDATION_ERROR"

        val fieldError = response.body?.errors?.first()
        fieldError?.field shouldBe "nickname"
        fieldError?.code shouldBe NicknameValidationException.TOO_SHORT
        fieldError?.message shouldBe "닉네임은 최소 2자 이상이어야 합니다"
    }

    test("PasswordValidationException은 400 BAD_REQUEST와 필드별 에러 정보 반환") {
        // Given
        val exception =
            PasswordValidationException(
                errorCode = PasswordValidationException.NO_SPECIAL_CHAR,
                message = "비밀번호는 특수문자를 포함해야 합니다",
            )

        // When
        val response = handler.handleValidationException(exception)

        // Then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.code shouldBe "VALIDATION_ERROR"

        val fieldError = response.body?.errors?.first()
        fieldError?.field shouldBe "password"
        fieldError?.code shouldBe PasswordValidationException.NO_SPECIAL_CHAR
        fieldError?.message shouldBe "비밀번호는 특수문자를 포함해야 합니다"
    }

    test("UserAlreadyExistsException.ForEmail은 409 CONFLICT 반환") {
        // Given
        val exception = UserAlreadyExistsException.ForEmail(Email("test@example.com"))

        // When
        val response = handler.handleUserAlreadyExists(exception)

        // Then
        response.statusCode shouldBe HttpStatus.CONFLICT
        response.body?.code shouldBe "USER_ALREADY_EXISTS"
        response.body?.message shouldBe "User with email test@example.com already exists"
    }

    test("UserAlreadyExistsException.ForNickname은 409 CONFLICT 반환") {
        // Given
        val exception = UserAlreadyExistsException.ForNickname(Nickname("existingUser"))

        // When
        val response = handler.handleUserAlreadyExists(exception)

        // Then
        response.statusCode shouldBe HttpStatus.CONFLICT
        response.body?.code shouldBe "USER_ALREADY_EXISTS"
        response.body?.message shouldBe "User with nickname existingUser already exists"
    }

    test("UserNotFoundException은 404 NOT_FOUND 반환") {
        // Given
        val exception = UserNotFoundException("user123")

        // When
        val response = handler.handleUserNotFound(exception)

        // Then
        response.statusCode shouldBe HttpStatus.NOT_FOUND
        response.body?.code shouldBe "USER_NOT_FOUND"
        response.body?.message shouldBe "User not found: user123"
    }

    test("InvalidCredentialsException은 401 UNAUTHORIZED 반환") {
        // Given
        val exception = InvalidCredentialsException()

        // When
        val response = handler.handleInvalidCredentials(exception)

        // Then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.body?.code shouldBe "INVALID_CREDENTIALS"
        response.body?.message shouldBe "Invalid email or password"
    }

    test("UserNotActiveException은 403 FORBIDDEN 반환") {
        // Given
        val exception = UserNotActiveException("User account is banned")

        // When
        val response = handler.handleUserNotActive(exception)

        // Then
        response.statusCode shouldBe HttpStatus.FORBIDDEN
        response.body?.code shouldBe "USER_NOT_ACTIVE"
        response.body?.message shouldBe "User account is banned"
    }

    test("PasswordMismatchException은 400 BAD_REQUEST 반환") {
        // Given
        val exception = PasswordMismatchException("Passwords do not match")

        // When
        val response = handler.handlePasswordMismatch(exception)

        // Then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.code shouldBe "PASSWORD_MISMATCH"
        response.body?.message shouldBe "Password and confirmation do not match: Passwords do not match"
    }

    test("PasswordPolicyViolationException은 400 BAD_REQUEST 반환") {
        // Given
        val exception = PasswordPolicyViolationException("Password too short")

        // When
        val response = handler.handlePasswordPolicyViolation(exception)

        // Then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.code shouldBe "PASSWORD_POLICY_VIOLATION"
        response.body?.message shouldBe "Password does not meet requirements: Password too short"
    }

    test("IllegalArgumentException은 400 BAD_REQUEST 반환") {
        // Given
        val exception = IllegalArgumentException("Invalid input")

        // When
        val response = handler.handleIllegalArgument(exception)

        // Then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.code shouldBe "INVALID_ARGUMENT"
        response.body?.message shouldBe "Invalid input"
    }

    test("RuntimeException은 500 INTERNAL_SERVER_ERROR 반환") {
        // Given
        val exception = RuntimeException("Something went wrong")

        // When
        val response = handler.handleRuntimeException(exception)

        // Then
        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        response.body?.code shouldBe "INTERNAL_ERROR"
        response.body?.message shouldBe "An unexpected error occurred"
    }

    test("일반 Exception은 500 INTERNAL_SERVER_ERROR 반환") {
        // Given
        val exception = Exception("Unexpected error")

        // When
        val response = handler.handleGenericException(exception)

        // Then
        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        response.body?.code shouldBe "INTERNAL_ERROR"
        response.body?.message shouldBe "An unexpected error occurred"
    }

    test("ErrorResponse는 timestamp를 포함") {
        // Given
        val response =
            ErrorResponse(
                code = "TEST_ERROR",
                message = "Test message",
            )

        // Then
        response.code shouldBe "TEST_ERROR"
        response.message shouldBe "Test message"
        (response.timestamp > 0) shouldBe true
    }

    test("ValidationErrorResponse는 필드별 에러 정보와 timestamp를 포함") {
        // Given
        val fieldErrors =
            listOf(
                FieldError(
                    field = "email",
                    code = "INVALID_FORMAT",
                    message = "Invalid email format",
                ),
                FieldError(
                    field = "password",
                    code = "TOO_SHORT",
                    message = "Password too short",
                ),
            )

        val response =
            ValidationErrorResponse(
                code = "VALIDATION_ERROR",
                message = "Validation failed",
                errors = fieldErrors,
            )

        // Then
        response.code shouldBe "VALIDATION_ERROR"
        response.message shouldBe "Validation failed"
        response.errors.size shouldBe 2
        response.errors[0].field shouldBe "email"
        response.errors[0].code shouldBe "INVALID_FORMAT"
        response.errors[1].field shouldBe "password"
        response.errors[1].code shouldBe "TOO_SHORT"
        (response.timestamp > 0) shouldBe true
    }
})
