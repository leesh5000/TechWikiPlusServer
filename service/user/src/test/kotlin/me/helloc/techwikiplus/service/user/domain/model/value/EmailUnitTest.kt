package me.helloc.techwikiplus.service.user.domain.model.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.EmailValidationException

class EmailUnitTest : FunSpec({

    test("유효한 이메일을 생성해야 한다") {
        val email = Email("user@example.com")
        email.value shouldBe "user@example.com"
    }

    test("null이거나 빈 이메일을 거부해야 한다") {
        val exception1 =
            shouldThrow<EmailValidationException> {
                Email("")
            }
        exception1.errorCode shouldBe EmailValidationException.BLANK_EMAIL
        exception1.field shouldBe "email"

        val exception2 =
            shouldThrow<EmailValidationException> {
                Email("   ")
            }
        exception2.errorCode shouldBe EmailValidationException.BLANK_EMAIL
    }

    test("잘못된 이메일 형식을 거부해야 한다 - @ 누락") {
        val exception =
            shouldThrow<EmailValidationException> {
                Email("userexample.com")
            }
        exception.errorCode shouldBe EmailValidationException.INVALID_FORMAT
        exception.field shouldBe "email"
    }

    test("잘못된 이메일 형식을 거부해야 한다 - 도메인 누락") {
        val exception =
            shouldThrow<EmailValidationException> {
                Email("user@")
            }
        exception.errorCode shouldBe EmailValidationException.INVALID_FORMAT
    }

    test("잘못된 이메일 형식을 거부해야 한다 - 로컬 부분 누락") {
        val exception =
            shouldThrow<EmailValidationException> {
                Email("@example.com")
            }
        exception.errorCode shouldBe EmailValidationException.INVALID_FORMAT
    }

    test("잘못된 이메일 형식을 거부해야 한다 - 잘못된 문자") {
        val exception =
            shouldThrow<EmailValidationException> {
                Email("user name@example.com")
            }
        exception.errorCode shouldBe EmailValidationException.INVALID_FORMAT
    }

    test("다양한 유효한 이메일 형식을 허용해야 한다") {
        Email("simple@example.com").value shouldBe "simple@example.com"
        Email("very.common@example.com").value shouldBe "very.common@example.com"
        Email("disposable.style.email.with+symbol@example.com").value shouldBe
            "disposable.style.email.with+symbol@example.com"
        Email("other.email-with-hyphen@example.com").value shouldBe "other.email-with-hyphen@example.com"
        Email("x@example.com").value shouldBe "x@example.com"
        Email("user@subdomain.example.com").value shouldBe "user@subdomain.example.com"
    }

    test("불변 객체여야 한다") {
        val email = Email("user@example.com")
        email.value shouldBe "user@example.com"
        // Value should not be modifiable (enforced by val property)
    }

    test("equals를 올바르게 구현해야 한다") {
        val email1 = Email("user@example.com")
        val email2 = Email("user@example.com")
        val email3 = Email("another@example.com")

        email1 shouldBe email2
        email1 shouldNotBe email3
        email1 shouldNotBe null
        email1 shouldNotBe "user@example.com"

        // 대소문자가 다른 이메일도 같은 것으로 취급해야 한다
        val email4 = Email("User@Example.COM")
        val email5 = Email("user@example.com")
        email4 shouldBe email5
    }

    test("hashCode를 올바르게 구현해야 한다") {
        val email1 = Email("user@example.com")
        val email2 = Email("user@example.com")
        val email3 = Email("another@example.com")

        email1.hashCode() shouldBe email2.hashCode()
        email1.hashCode() shouldNotBe email3.hashCode()

        // 대소문자가 다른 이메일도 같은 hashCode를 가져야 한다
        val email4 = Email("User@Example.COM")
        val email5 = Email("user@example.com")
        email4.hashCode() shouldBe email5.hashCode()
    }

    test("toString을 올바르게 구현해야 한다") {
        val email = Email("user@example.com")
        email.toString() shouldBe "Email(value=user@example.com)"
    }

    test("이메일은 항상 소문자로 정규화되어야 한다") {
        val email1 = Email("User@Example.COM")
        email1.value shouldBe "user@example.com"

        val email2 = Email("ADMIN@DOMAIN.ORG")
        email2.value shouldBe "admin@domain.org"

        val email3 = Email("MixedCase@Test.Net")
        email3.value shouldBe "mixedcase@test.net"
    }
})
