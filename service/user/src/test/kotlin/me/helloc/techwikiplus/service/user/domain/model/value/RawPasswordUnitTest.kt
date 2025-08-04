package me.helloc.techwikiplus.service.user.domain.model.value

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.helloc.techwikiplus.service.user.domain.exception.PasswordValidationException

class RawPasswordUnitTest : FunSpec({

    test("유효한 비밀번호로 RawPassword 객체 생성 성공") {
        val validPassword = "Password123!"

        shouldNotThrow<Throwable> {
            val rawPassword = RawPassword(validPassword)
            rawPassword.value shouldBe validPassword
        }
    }

    test("비밀번호가 비어있으면 BLANK_PASSWORD 예외 발생") {
        val exception =
            shouldThrow<PasswordValidationException> {
                RawPassword("")
            }

        exception.errorCode shouldBe PasswordValidationException.BLANK_PASSWORD
        exception.message shouldContain "필수 입력 항목"
    }

    test("비밀번호가 공백으로만 이루어져 있으면 BLANK_PASSWORD 예외 발생") {
        val exception =
            shouldThrow<PasswordValidationException> {
                RawPassword("   ")
            }

        exception.errorCode shouldBe PasswordValidationException.BLANK_PASSWORD
        exception.message shouldContain "필수 입력 항목"
    }

    test("비밀번호가 8자 미만이면 TOO_SHORT 예외 발생") {
        val exception =
            shouldThrow<PasswordValidationException> {
                RawPassword("Pass1!")
            }

        exception.errorCode shouldBe PasswordValidationException.TOO_SHORT
        exception.message shouldContain "최소 8자 이상"
    }

    test("비밀번호가 30자 초과하면 TOO_LONG 예외 발생") {
        val exception =
            shouldThrow<PasswordValidationException> {
                RawPassword("a".repeat(31) + "A1!")
            }

        exception.errorCode shouldBe PasswordValidationException.TOO_LONG
        exception.message shouldContain "최대 30자 이하"
    }

    test("비밀번호에 대문자가 없으면 NO_UPPERCASE 예외 발생") {
        val exception =
            shouldThrow<PasswordValidationException> {
                RawPassword("password123!")
            }

        exception.errorCode shouldBe PasswordValidationException.NO_UPPERCASE
        exception.message shouldContain "대문자를 포함"
    }

    test("비밀번호에 소문자가 없으면 NO_LOWERCASE 예외 발생") {
        val exception =
            shouldThrow<PasswordValidationException> {
                RawPassword("PASSWORD123!")
            }

        exception.errorCode shouldBe PasswordValidationException.NO_LOWERCASE
        exception.message shouldContain "소문자를 포함"
    }

    test("비밀번호에 특수문자가 없으면 NO_SPECIAL_CHAR 예외 발생") {
        val exception =
            shouldThrow<PasswordValidationException> {
                RawPassword("Password123")
            }

        exception.errorCode shouldBe PasswordValidationException.NO_SPECIAL_CHAR
        exception.message shouldContain "특수문자를 포함"
    }

    test("다양한 특수문자를 포함한 비밀번호 허용") {
        val specialChars = "!@#$%^&*()-_=+[]{}|;:'\",.<>?/~`"
        specialChars.forEach { specialChar ->
            shouldNotThrow<Throwable> {
                RawPassword("Password123$specialChar")
            }
        }
    }

    test("equals 메서드가 올바르게 동작") {
        val password1 = RawPassword("Password123!")
        val password2 = RawPassword("Password123!")
        val password3 = RawPassword("Different123!")

        (password1 == password2) shouldBe true
        (password1 == password3) shouldBe false
        (password1 == password1) shouldBe true
        (password1.equals(null)) shouldBe false
        (password1.equals("Password123!")) shouldBe false
    }

    test("hashCode 메서드가 올바르게 동작") {
        val password1 = RawPassword("Password123!")
        val password2 = RawPassword("Password123!")

        password1.hashCode() shouldBe password2.hashCode()
    }

    test("toString 메서드는 비밀번호를 마스킹하여 반환") {
        val password = RawPassword("Password123!")

        password.toString() shouldBe "RawPassword(****)"
    }

    test("경계값 테스트 - 정확히 8자인 비밀번호") {
        shouldNotThrow<Throwable> {
            RawPassword("Pass12!A")
        }
    }

    test("경계값 테스트 - 정확히 30자인 비밀번호") {
        val exactlyThirtyChars = "a".repeat(25) + "A12!x"
        shouldNotThrow<Throwable> {
            RawPassword(exactlyThirtyChars)
        }
    }
})
