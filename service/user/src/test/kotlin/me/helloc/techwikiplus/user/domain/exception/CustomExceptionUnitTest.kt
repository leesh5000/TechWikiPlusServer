package me.helloc.techwikiplus.user.domain.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidEmail
import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidNickname

class CustomExceptionUnitTest : FunSpec({

    context("ValidationException") {
        test("InvalidEmail 예외") {
            val email = "invalid@"
            val exception = InvalidEmail(email)

            exception.email shouldBe email
            exception.message shouldBe "Invalid email format. Your input: $email"
            exception.shouldBeInstanceOf<CustomException.ValidationException>()
            exception.shouldBeInstanceOf<CustomException>()
            exception.shouldBeInstanceOf<RuntimeException>()
        }

        test("InvalidNickname 예외") {
            val nickname = "!"
            val exception = InvalidNickname(nickname)

            exception.nickname shouldBe nickname
            exception.message shouldBe "Nickname must be 2-20 characters long and can only contain alphanumeric characters and Korean characters. Your input: $nickname"
            exception.shouldBeInstanceOf<CustomException.ValidationException>()
            exception.shouldBeInstanceOf<CustomException>()
            exception.shouldBeInstanceOf<RuntimeException>()
        }

        test("다양한 잘못된 닉네임 케이스") {
            val invalidNicknames = listOf(
                "a",           // 너무 짧음
                "a".repeat(21), // 너무 김
                "test@user",   // 특수문자
                "test user",   // 공백
                "test!",       // 느낌표
                ""             // 빈 문자열
            )

            invalidNicknames.forEach { nickname ->
                val exception = InvalidNickname(nickname)
                exception.nickname shouldBe nickname
                exception.message shouldBe "Nickname must be 2-20 characters long and can only contain alphanumeric characters and Korean characters. Your input: $nickname"
            }
        }

        test("다양한 잘못된 이메일 케이스") {
            val invalidEmails = listOf(
                "invalid-email",
                "@example.com",
                "user@",
                "user@.com",
                "user@example",
                "user @example.com",
                "user@example com",
                ""
            )

            invalidEmails.forEach { email ->
                val exception = InvalidEmail(email)
                exception.email shouldBe email
                exception.message shouldBe "Invalid email format. Your input: $email"
            }
        }
    }

    context("예외 계층 구조") {
        test("ValidationException은 CustomException을 상속") {
            val emailException = InvalidEmail("test")
            val nicknameException = InvalidNickname("!")

            emailException.shouldBeInstanceOf<CustomException>()
            nicknameException.shouldBeInstanceOf<CustomException>()
        }

        test("모든 CustomException은 RuntimeException을 상속") {
            val emailException = InvalidEmail("test")
            val nicknameException = InvalidNickname("!")

            emailException.shouldBeInstanceOf<RuntimeException>()
            nicknameException.shouldBeInstanceOf<RuntimeException>()
        }

        test("sealed class로 타입 안전성 보장") {
            val exception: CustomException = InvalidEmail("test")
            when (exception) {
                is CustomException.ValidationException -> {
                    // ValidationException 처리
                    when (exception) {
                        is InvalidEmail -> {
                            exception.email shouldBe "test"
                        }
                        is InvalidNickname -> {
                            // 이 케이스는 실행되지 않음
                        }
                        is CustomException.ValidationException.InvalidPassword -> {
                            // 이 케이스는 실행되지 않음
                        }
                    }
                }
                is CustomException.NotFoundException -> {
                    // NotFoundException 처리 (현재는 비어있음)
                }
                is CustomException.ConflictException -> {
                    // ConflictException 처리 (현재는 비어있음)
                }
                is CustomException.AuthenticationException -> {
                    // AuthenticationException 처리 (현재는 비어있음)
                }
                is CustomException.ResendRateLimitExceeded -> {
                    // ResendRateLimitExceeded 처리 (현재는 비어있음)
                }
            }
        }
    }

    context("예외 메시지 검증") {
        test("InvalidEmail 메시지 형식") {
            val email = "test@invalid"
            val exception = InvalidEmail(email)

            exception.message shouldBe "Invalid email format. Your input: $email"
        }

        test("InvalidNickname 메시지 형식") {
            val nickname = "ab@cd"
            val exception = InvalidNickname(nickname)

            exception.message shouldBe "Nickname must be 2-20 characters long and can only contain alphanumeric characters and Korean characters. Your input: $nickname"
        }

        test("예외 메시지에 실제 입력값 포함") {
            val testCases = mapOf(
                "invalid@" to InvalidEmail::class,
                "!" to InvalidNickname::class,
                "" to InvalidEmail::class,
                "toolongnicknamethatwillcauseerror" to InvalidNickname::class
            )

            testCases.forEach { (input, exceptionClass) ->
                val exception = when (exceptionClass) {
                    InvalidEmail::class -> InvalidEmail(input)
                    InvalidNickname::class -> InvalidNickname(input)
                    else -> throw IllegalArgumentException("Unknown exception class")
                }

                exception.message.contains(input) shouldBe true
            }
        }
    }

    context("예외 데이터 접근") {
        test("InvalidEmail에서 잘못된 이메일 값 접근 가능") {
            val wrongEmail = "wrong@email"
            val exception = InvalidEmail(wrongEmail)

            exception.email shouldBe wrongEmail
        }

        test("InvalidNickname에서 잘못된 닉네임 값 접근 가능") {
            val wrongNickname = "wrong!"
            val exception = InvalidNickname(wrongNickname)

            exception.nickname shouldBe wrongNickname
        }
    }
})
