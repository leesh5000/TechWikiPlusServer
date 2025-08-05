package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.PasswordMismatchException
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword

class PasswordConfirmationVerifierUnitTest : FunSpec(
    {

        test("비밀번호 확인 검증") {
            // Given
            val passwordConfirmationVerifier = PasswordConfirmationVerifier()

            // When
            val rawPassword = RawPassword("Password123!")
            val passwordConfirmation = RawPassword("Password123!")

            // Then
            passwordConfirmationVerifier.equalsOrThrows(rawPassword, passwordConfirmation)
        }

        test("비밀번호와 확인이 일치하지 않을 때 예외 발생") {
            // Given
            val passwordConfirmationVerifier = PasswordConfirmationVerifier()

            // When
            val rawPassword = RawPassword("Password123!")
            val passwordConfirmation = RawPassword("DifferentPassword123!")

            // Then
            shouldThrow<PasswordMismatchException> {
                passwordConfirmationVerifier.equalsOrThrows(rawPassword, passwordConfirmation)
            }
        }

        test("비밀번호 확인 검증 실패 시 예외 메시지 확인") {
            // Given
            val passwordConfirmationVerifier = PasswordConfirmationVerifier()

            // When
            val rawPassword = RawPassword("Password123!")
            val passwordConfirmation = RawPassword("DifferentPassword123!")

            // Then
            val exception =
                shouldThrow<PasswordMismatchException> {
                    passwordConfirmationVerifier.equalsOrThrows(rawPassword, passwordConfirmation)
                }
            exception.message shouldBe "Password and confirmation do not match: Password and confirmation do not match."
        }
    },
)
