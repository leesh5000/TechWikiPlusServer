package me.helloc.techwikiplus.user.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.user.domain.exception.CustomException.ValidationException.InvalidEmail

class UserEmailUnitTest : FunSpec({

    context("UserEmail 생성") {
        test("유효한 이메일로 생성 성공") {
            val validEmails = listOf(
                "user@example.com",
                "test.user@example.com",
                "user+tag@example.co.kr",
                "user123@subdomain.example.com",
                "user_name@example.org"
            )

            validEmails.forEach { email ->
                val userEmail = UserEmail(email)
                userEmail.value shouldBe email
                userEmail.verified shouldBe false
            }
        }

        test("검증된 이메일로 생성") {
            val email = UserEmail("test@example.com", true)
            email.value shouldBe "test@example.com"
            email.verified shouldBe true
        }

        test("잘못된 이메일 형식으로 생성 시 InvalidEmail 예외 발생") {
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
                shouldThrow<InvalidEmail> {
                    UserEmail(email)
                }.email shouldBe email
            }
        }
    }

    context("UserEmail 메서드") {
        test("verify() 메서드로 인증된 이메일 생성") {
            val email = UserEmail("test@example.com")
            val verifiedEmail = email.verify()

            verifiedEmail.value shouldBe email.value
            verifiedEmail.verified shouldBe true
            email.verified shouldBe false // 원본은 변경되지 않음
        }

        test("toString()은 이메일 값 반환") {
            val email = UserEmail("test@example.com")
            email.toString() shouldBe "test@example.com"
        }

        test("동일한 이메일 값의 UserEmail은 동등") {
            val email1 = UserEmail("test@example.com")
            val email2 = UserEmail("test@example.com")

            email1 shouldBe email2
            email1.hashCode() shouldBe email2.hashCode()
        }

        test("다른 이메일 값의 UserEmail은 동등하지 않음") {
            val email1 = UserEmail("user1@example.com")
            val email2 = UserEmail("user2@example.com")

            email1 shouldNotBe email2
        }

        test("검증 상태가 다른 동일 이메일은 다름") {
            val unverifiedEmail = UserEmail("test@example.com", false)
            val verifiedEmail = UserEmail("test@example.com", true)

            unverifiedEmail shouldNotBe verifiedEmail
        }
    }

    context("UserEmail companion object") {
        test("isValid 메서드로 이메일 형식 검증") {
            UserEmail.isValid("test@example.com") shouldBe true
            UserEmail.isValid("invalid-email") shouldBe false
            UserEmail.isValid("@example.com") shouldBe false
            UserEmail.isValid("test@") shouldBe false
        }
    }
})