package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.PasswordMismatchException
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword

class PasswordConfirmationVerifierTest : FunSpec(
    {

        test("비밀번호 확인 검증") {
            // Given
            val passwordConfirmationVerifier = PasswordConfirmationVerifier()

            // When
            val rawPassword = "Password123!"
            val passwordConfirmation = "Password123!"
            EncodedPassword.validateRawPasswordPolicy(rawPassword)
            EncodedPassword.validateRawPasswordPolicy(passwordConfirmation)

            // Then
            passwordConfirmationVerifier.verify(rawPassword, passwordConfirmation)
        }

        test("비밀번호와 확인이 일치하지 않을 때 예외 발생") {
            // Given
            val passwordConfirmationVerifier = PasswordConfirmationVerifier()

            // When
            val rawPassword = "Password123!"
            val passwordConfirmation = "DifferentPassword123!"
            EncodedPassword.validateRawPasswordPolicy(rawPassword)
            EncodedPassword.validateRawPasswordPolicy(passwordConfirmation)

            // Then
            shouldThrow<PasswordMismatchException> {
                passwordConfirmationVerifier.verify(rawPassword, passwordConfirmation)
            }
        }

        test("비밀번호 확인 검증 실패 시 예외 메시지 확인") {
            // Given
            val passwordConfirmationVerifier = PasswordConfirmationVerifier()

            // When
            val rawPassword = "Password123!"
            val passwordConfirmation = "DifferentPassword123!"
            EncodedPassword.validateRawPasswordPolicy(rawPassword)
            EncodedPassword.validateRawPasswordPolicy(passwordConfirmation)

            // Then
            val exception =
                shouldThrow<PasswordMismatchException> {
                    passwordConfirmationVerifier.verify(rawPassword, passwordConfirmation)
                }
            exception.message shouldBe "Password and confirmation do not match."
        }
    },
)
