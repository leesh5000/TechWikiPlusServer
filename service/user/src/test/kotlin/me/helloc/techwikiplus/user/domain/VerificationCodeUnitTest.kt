package me.helloc.techwikiplus.user.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidVerificationCodeException

class VerificationCodeUnitTest : FunSpec({

    context("VerificationCode 생성") {
        test("유효한 6자리 알파벳 대문자로 생성 성공") {
            val validCodes =
                listOf(
                    "ABCDEF",
                    "XYZSAB",
                    "HELLO1",
                    "ABC123",
                )

            validCodes.forEach { code ->
                val verificationCode = VerificationCode(code)
                verificationCode.value shouldBe code
            }
        }

        test("빈 문자열로 생성 시 예외 발생") {
            shouldThrow<IllegalArgumentException> {
                VerificationCode("")
            }.message shouldBe "Verification code must not be blank."
        }

        test("공백 문자열로 생성 시 예외 발생") {
            shouldThrow<IllegalArgumentException> {
                VerificationCode("   ")
            }.message shouldBe "Verification code must not be blank."
        }

        test("6자리가 아닌 경우 예외 발생") {
            val invalidLengthCodes =
                listOf(
                    "A",
                    "AB",
                    "ABC",
                    "ABCD",
                    "ABCDE",
                    "ABCDEFG",
                    "ABCDEFGH",
                )

            invalidLengthCodes.forEach { code ->
                shouldThrow<IllegalArgumentException> {
                    VerificationCode(code)
                }.message shouldBe "Verification code must be exactly 6 characters long."
            }
        }

        test("특수문자 포함 시 예외 발생") {
            val invalidCharCodes =
                listOf(
                    "ABC@EF",
                    "ABC#EF",
                    "ABC EF",
                    "ABC-EF",
                    "ABC.EF",
                    "ABC!EF",
                )

            invalidCharCodes.forEach { code ->
                shouldThrow<IllegalArgumentException> {
                    VerificationCode(code)
                }.message shouldBe "Verification code must contain only letters and digits."
            }
        }
    }

    context("VerificationCode generate 메서드") {
        test("generate()는 6자리 대문자 알파벳 코드를 생성") {
            repeat(100) { // 여러 번 실행하여 무작위성 확인
                val code = VerificationCode.generate()

                code.value shouldHaveLength 6
                code.value shouldMatch "[A-Z]{6}".toRegex()
            }
        }

        test("generate()로 생성된 코드는 매번 다름") {
            val generatedCodes = mutableSetOf<String>()

            repeat(10) {
                generatedCodes.add(VerificationCode.generate().value)
            }

            // 10번 생성 시 적어도 2개 이상의 다른 코드가 생성되어야 함
            generatedCodes.size shouldBe 10
        }
    }

    context("VerificationCode equalsOrThrows 메서드") {
        test("일치하는 코드 비교 시 예외 발생하지 않음") {
            val verificationCode = VerificationCode("ABCDEF")

            // 예외가 발생하지 않으면 테스트 통과
            verificationCode.equalsOrThrows("ABCDEF")
        }

        test("일치하지 않는 코드 비교 시 InvalidVerificationCodeException 예외 발생") {
            val verificationCode = VerificationCode("ABCDEF")

            shouldThrow<InvalidVerificationCodeException> {
                verificationCode.equalsOrThrows("WRONG1")
            }.invalidCode shouldBe "WRONG1"
        }

        test("대소문자 구분하여 비교") {
            val verificationCode = VerificationCode("ABCDEF")

            shouldThrow<InvalidVerificationCodeException> {
                verificationCode.equalsOrThrows("abcdef")
            }.invalidCode shouldBe "abcdef"
        }
    }
})
